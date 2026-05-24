package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.CrmDatabase
import com.example.database.CrmRepository
import com.example.database.Lead
import com.example.database.LeadNote
import com.example.database.MessageTemplate
import com.example.database.TimelineEvent
import com.example.data.GeminiHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SelectedScreen(val title: String) {
    DASHBOARD("Dashboard"),
    NEW_LEADS("New Leads"),
    FOLLOW_UP("Follow-up Pipeline"),
    INTERESTED_LEADS("Interested Leads"),
    REPLIED_LEADS("Replied Leads"),
    CLOSED_CLIENTS("Closed Retainers"),
    NOT_INTERESTED("Not Interested"),
    ANALYTICS("Analytics Hub"),
    WHATSAPP_TEMPLATES("WhatsApp Templates"),
    SETTINGS("CRM Settings")
}

data class WhatsAppPopupState(
    val lead: Lead,
    val templateName: String,
    val messageText: String
)

data class CrmNotification(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class AdCampaignPerformance(
    val id: Int,
    val name: String,
    val spend: Double,
    val leads: Int,
    val cpl: Double,
    val roi: Double,
    val status: String // "Active" or "Paused"
)

data class SmmFilterParams(
    val query: String,
    val order: String,
    val priority: String?,
    val source: String?
)

class CrmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CrmRepository

    // Base UI and Dark mode settings
    val isLightMode = MutableStateFlow(false)

    // Current Navigation Screen Section
    val selectedScreen = MutableStateFlow(SelectedScreen.DASHBOARD)

    // Search and filters
    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow("Date Added (Newest)")
    val priorityFilter = MutableStateFlow<String?>(null)
    val leadSourceFilter = MutableStateFlow<String?>(null)

    // Leads list flow
    private val _allLeads = MutableStateFlow<List<Lead>>(emptyList())
    val allLeads: StateFlow<List<Lead>> = _allLeads.asStateFlow()

    // Message templates flow
    private val _allTemplates = MutableStateFlow<List<MessageTemplate>>(emptyList())
    val allTemplates: StateFlow<List<MessageTemplate>> = _allTemplates.asStateFlow()

    // Slide Panel Lead detail state
    val selectedLead = MutableStateFlow<Lead?>(null)

    // Secondary detail states
    private val _selectedLeadNotes = MutableStateFlow<List<LeadNote>>(emptyList())
    val selectedLeadNotes: StateFlow<List<LeadNote>> = _selectedLeadNotes.asStateFlow()

    private val _selectedLeadTimeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val selectedLeadTimeline: StateFlow<List<TimelineEvent>> = _selectedLeadTimeline.asStateFlow()

    // Active AI / Manual Template Popups
    val pendingWhatsAppPopup = MutableStateFlow<WhatsAppPopupState?>(null)
    val isAiGeneratingPitch = MutableStateFlow(false)

    // Live SMM Notifications & Reminders
    val notifications = MutableStateFlow<List<CrmNotification>>(emptyList())

    // Admin lists
    val teamMembers = listOf("Abhiraj Gupta", "Sarah Connor", "John Doe", "Alex Rivera")
    val leadSources = listOf("Meta Ads", "LinkedIn Outreach", "Organic Instagram", "Google Search", "Client Referral")

    // Mutable Ad Spend total (can be tweaked in settings)
    val totalAdSpend = MutableStateFlow(42500.0)

    // Meta Ad campaign metrics
    val adCampaigns = listOf(
        AdCampaignPerformance(1, "SMM High ROAS - Spring Collection", 12500.0, 20, 625.0, 4.2, "Active"),
        AdCampaignPerformance(2, "Luxury Villas B2B Outreach", 15000.0, 10, 1500.0, 5.8, "Active"),
        AdCampaignPerformance(3, "Local Dental Implants Inflow", 10000.0, 15, 666.0, 3.5, "Active"),
        AdCampaignPerformance(4, "Premium Beauty Salon Lead-Form", 5000.0, 8, 625.0, 2.4, "Paused")
    )

    init {
        val database = CrmDatabase.getDatabase(application)
        repository = CrmRepository(database.crmDao())

        // Setup notification list
        loadInitialNotifications()

        viewModelScope.launch {
            // Seed DB on first run to give beautiful visual sandbox
            repository.prepopulateIfEmpty(application)

            // Collect active items
            launch {
                repository.allLeads.collect { leadsList ->
                    _allLeads.value = leadsList
                }
            }

            launch {
                repository.allTemplates.collect { templateList ->
                    _allTemplates.value = templateList
                }
            }

            // Sync slide-panel detail items reactively
            launch {
                selectedLead.collect { lead ->
                    if (lead != null) {
                        repository.getNotesForLead(lead.id).collect { notes ->
                            _selectedLeadNotes.value = notes
                        }
                    } else {
                        _selectedLeadNotes.value = emptyList()
                    }
                }
            }

            launch {
                selectedLead.collect { lead ->
                    if (lead != null) {
                        repository.getTimelineForLead(lead.id).collect { timeline ->
                            _selectedLeadTimeline.value = timeline
                        }
                    } else {
                        _selectedLeadTimeline.value = emptyList()
                    }
                }
            }
        }
    }

    private fun loadInitialNotifications() {
        notifications.value = listOf(
            CrmNotification(
                id = 1,
                title = "Follow-up due: Priya Patel",
                message = "Brief Zoom SMM strategy call requires scheduling. Priya expressed warm interest via local Ads.",
                timestamp = "3 mins ago"
            ),
            CrmNotification(
                id = 2,
                title = "Critical lead: Rohan Kapoor",
                message = "High value D2C Gym Nutrition lead scheduled for Meeting. Review their past influencer reels.",
                timestamp = "1 hour ago"
            ),
            CrmNotification(
                id = 3,
                title = "Reminder: Vikram Malhotra",
                message = "Action needed: Vikram replied to our initial LinkedIn message. WhatsApp follow-up pending.",
                timestamp = "Yesterday"
            )
        )
    }

    // Compose filtering & searching
    val filteredLeads: StateFlow<List<Lead>> = combine(
        allLeads,
        selectedScreen,
        combine(searchQuery, sortOrder, priorityFilter, leadSourceFilter) { q, o, pf, sf ->
            SmmFilterParams(q, o, pf, sf)
        }
    ) { leads, screen, params ->
        var list = leads

        // Process Screen status filtering
        list = when (screen) {
            SelectedScreen.NEW_LEADS -> list.filter { it.status == "New Lead" }
            SelectedScreen.FOLLOW_UP -> list.filter { it.status == "Follow-up" || it.status == "No Reply" }
            SelectedScreen.INTERESTED_LEADS -> list.filter { it.status == "Interested" || it.status == "Meeting Scheduled" }
            SelectedScreen.REPLIED_LEADS -> list.filter { it.status == "Replied" || it.status == "Contacted" }
            SelectedScreen.CLOSED_CLIENTS -> list.filter { it.status == "Closed" }
            SelectedScreen.NOT_INTERESTED -> list.filter { it.status == "Not Interested" }
            else -> list // Dashboard, Analytics etc see everything
        }

        // Search Query filter
        if (params.query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(params.query, ignoreCase = true) ||
                it.phone.contains(params.query, ignoreCase = true) ||
                it.businessType.contains(params.query, ignoreCase = true) ||
                it.adCampaign.contains(params.query, ignoreCase = true)
            }
        }

        // Extra dynamic filters
        params.priority?.let { p ->
            list = list.filter { it.priority.equals(p, ignoreCase = true) }
        }
        params.source?.let { s ->
            list = list.filter { it.leadSource.equals(s, ignoreCase = true) }
        }

        // Sorting options
        list = when (params.order) {
            "Budget (High to Low)" -> list.sortedByDescending { it.budget }
            "Budget (Low to High)" -> list.sortedBy { it.budget }
            "AI Score (High to Low)" -> list.sortedByDescending { it.aiLeadScore }
            "Priority" -> list.sortedBy {
                when (it.priority) {
                    "High" -> 0
                    "Medium" -> 1
                    else -> 2
                }
            }
            "Date Added (Oldest)" -> list.sortedBy { it.dateAdded }
            else -> list.sortedByDescending { it.dateAdded } // Date Added (Newest)
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations for KPIs
    val totalLeadsCount = allLeads.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val todayLeadsCount = allLeads.map { list ->
        // Return counts added today (within last 24h for simulator ease)
        list.filter { System.currentTimeMillis() - it.dateAdded < 24 * 3600 * 1000 }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val interestedLeadsCount = allLeads.map { list ->
        list.filter { it.status == "Interested" || it.status == "Meeting Scheduled" }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val followUpLeadsCount = allLeads.map { list ->
        list.filter { it.status == "Follow-up" || it.status == "No Reply" }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val closedRetainersCount = allLeads.map { list ->
        list.filter { it.status == "Closed" }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // SMM Closed retainers MRR sum. When status changes, revenue updates instantly!
    val totalRevenueGenerated = allLeads.map { list ->
        list.filter { it.status == "Closed" }.sumOf { it.budget }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Cost per SMM Lead averages live
    val costPerLead = combine(totalAdSpend, totalLeadsCount) { spend, count ->
        if (count > 0) spend / count else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Add lead action (creates detailed logging timeline & inserts)
     */
    fun addNewLead(
        name: String,
        phone: String,
        businessType: String,
        leadSource: String,
        budget: Double,
        assignedTeam: String,
        campaign: String = "",
        priority: String = "Medium"
    ) {
        viewModelScope.launch {
            // Perform basic pre-score
            val initScore = if (budget >= 50000) 90 else if (budget >= 25000) 75 else 55
            
            val lead = Lead(
                name = name,
                phone = phone,
                businessType = businessType,
                leadSource = leadSource,
                budget = budget,
                status = "New Lead",
                assignedTeam = assignedTeam,
                adCampaign = campaign,
                priority = priority,
                aiLeadScore = initScore
            )
            repository.insertLead(lead)
            
            // Add initial system notification
            val newNotification = CrmNotification(
                id = (notifications.value.maxOfOrNull { it.id } ?: 0) + 1,
                title = "New SMM Lead: $name",
                message = "New lead captured via $leadSource. Assigned to $assignedTeam.",
                timestamp = "Just now"
            )
            notifications.value = listOf(newNotification) + notifications.value
        }
    }

    /**
     * Updates leads status instantly. Relocates lead, logs timelines and notes.
     * Triggers WhatsApp Pitch Dialog automatically if changing status to "Interested", "Follow-up", or "Not Interested" or "No Reply"
     */
    fun changeLeadStatus(lead: Lead, newStatus: String) {
        viewModelScope.launch {
            repository.updateLeadStatus(lead, newStatus)
            
            // Sync selectedLead detail view
            if (selectedLead.value?.id == lead.id) {
                selectedLead.value = lead.copy(status = newStatus)
            }

            // Create notification of change
            val newNotification = CrmNotification(
                id = (notifications.value.maxOfOrNull { it.id } ?: 0) + 1,
                title = "Pipeline update: ${lead.name}",
                message = "Status shifted to $newStatus.",
                timestamp = "Just now"
            )
            notifications.value = listOf(newNotification) + notifications.value

            // Auto-trigger WhatsApp Templates templates Pop-up for Sales workflows
            if (newStatus == "Interested" || newStatus == "Follow-up" || newStatus == "Not Interested" || newStatus == "No Reply") {
                triggerTemplatePopup(lead, newStatus)
            }
        }
    }

    /**
     * Manages SMM Private notes writing
     */
    fun addLeadNote(leadId: Int, content: String) {
        viewModelScope.launch {
            repository.addNote(leadId, content)
            
            // Re-trigger visual sync
            selectedLead.value?.let { lead ->
                selectedLead.value = lead.copy() // trigger recompose flow
            }
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
            if (selectedLead.value?.id == lead.id) {
                selectedLead.value = null
            }
        }
    }

    fun markNotificationAsRead(id: Int) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun addTimelineEvent(leadId: Int, title: String, description: String, iconType: String) {
        viewModelScope.launch {
            repository.addCustomTimelineEvent(leadId, title, description, iconType)
        }
    }

    /**
     * Prepares and autofills WhatsApp Template to present to the user
     */
    private suspend fun triggerTemplatePopup(lead: Lead, status: String) {
        // Fetch active templates to find an applicable SMM template
        val temps = allTemplates.first()
        val template = temps.firstOrNull { it.category.equals(status, ignoreCase = true) }
            ?: temps.firstOrNull { it.category.equals("Interested", ignoreCase = true) }

        val formatText = if (template != null) {
            template.content
                .replace("{name}", lead.name)
                .replace("{business}", lead.businessType)
                .replace("{source}", lead.leadSource)
                .replace("{budget}", String.format("₹%,.0f", lead.budget))
        } else {
            // Hardcode default fallback pitches directly based on status
            when (status) {
                "Follow-up" -> "Hello ${lead.name}, I am following up on your inquiry about SMM services. Let me know if you would like to have a quick call this week."
                "Not Interested" -> "Thank you for your time, ${lead.name}! If you ever need SMM or Meta/Instagram Ads scaling in the future, we would love to partner with you."
                else -> "Hello ${lead.name}, thank you for your SMM Agency interest! We noticed you run ${lead.businessType}..."
            }
        }

        pendingWhatsAppPopup.value = WhatsAppPopupState(
            lead = lead,
            templateName = template?.name ?: "SMM status $status template",
            messageText = formatText
        )
    }

    /**
     * Gemini AI Smart Pitch Generation
     */
    fun runGeminiCampaignPitch(lead: Lead) {
        viewModelScope.launch {
            isAiGeneratingPitch.value = true
            try {
                val smmPitch = GeminiHelper.generateSmmPitch(
                    leadName = lead.name,
                    businessType = lead.businessType,
                    source = lead.leadSource,
                    budget = lead.budget,
                    priority = lead.priority
                )
                
                // Add trace in lead timeline that SMM campaign proposal idea generated
                repository.addCustomTimelineEvent(
                    leadId = lead.id,
                    title = "AI Pitch Formulated",
                    description = "Gemini AI generated customizable growth proposal idea.",
                    iconType = "system"
                )

                pendingWhatsAppPopup.value = WhatsAppPopupState(
                    lead = lead,
                    templateName = "Gemini AI SMM Tailored Pitch",
                    messageText = smmPitch
                )
            } catch (e: Exception) {
                Log.e("CrmViewModel", "AI Pitch generation error", e)
            } finally {
                isAiGeneratingPitch.value = false
            }
        }
    }

    /**
     * Gemini AI CRM Lead Score & Strategy Audit
     */
    fun runGeminiLeadAudit(lead: Lead) {
        viewModelScope.launch {
            isAiGeneratingPitch.value = true
            try {
                val result = GeminiHelper.analyzeLeadAndScore(
                    leadName = lead.name,
                    businessType = lead.businessType,
                    source = lead.leadSource,
                    budget = lead.budget,
                    adCampaign = lead.adCampaign
                )

                // Update lead's AI Score dynamically
                val updatedLead = lead.copy(aiLeadScore = result.first)
                repository.updateLeadDetails(updatedLead)

                // Add timeline event
                repository.addCustomTimelineEvent(
                    leadId = lead.id,
                    title = "AI Lead Health Audit",
                    description = "ROI Score: ${result.first}% | Strategy: ${result.second}",
                    iconType = "system"
                )

                // Add a permanent Note
                repository.addNote(
                    leadId = lead.id,
                    content = "[AI Audit] Score: ${result.first}% - Recommendation: ${result.second}",
                    author = "Gemini AI"
                )

                // Sync current opened lead
                selectedLead.value = updatedLead

                // Create alert notification
                val newNotification = CrmNotification(
                    id = (notifications.value.maxOfOrNull { it.id } ?: 0) + 1,
                    title = "AI Lead Audit Complete: ${lead.name}",
                    message = "ROI Score evaluated at ${result.first}%. Smart pitch strategy registered.",
                    timestamp = "Just now"
                )
                notifications.value = listOf(newNotification) + notifications.value

            } catch (e: Exception) {
                Log.e("CrmViewModel", "AI Lead score audit error", e)
            } finally {
                isAiGeneratingPitch.value = false
            }
        }
    }

    // Add Templates
    fun addWhatsAppTemplate(name: String, category: String, content: String) {
        viewModelScope.launch {
            val temp = MessageTemplate(name = name, category = category, content = content)
            repository.insertTemplate(temp)
        }
    }

    fun deleteWhatsAppTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun dismissWhatsAppPopup() {
        pendingWhatsAppPopup.value = null
    }

    /**
     * Marks a WhatsApp outreach simulation log
     */
    fun logWhatsAppOutreachSent(lead: Lead) {
        viewModelScope.launch {
            repository.addCustomTimelineEvent(
                leadId = lead.id,
                title = "WhatsApp Pitch Sent",
                description = "Custom message outreach triggered to client ${lead.name}",
                iconType = "whatsapp"
            )
        }
    }
}
