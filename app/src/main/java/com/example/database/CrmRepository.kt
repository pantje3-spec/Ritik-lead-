package com.example.database

import android.content.Context
import com.example.database.Lead
import com.example.database.TimelineEvent
import com.example.database.LeadNote
import com.example.database.MessageTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrmRepository(private val crmDao: CrmDao) {

    val allLeads: Flow<List<Lead>> = crmDao.getAllLeads()
    val allTemplates: Flow<List<MessageTemplate>> = crmDao.getAllTemplates()

    fun getNotesForLead(leadId: Int): Flow<List<LeadNote>> = crmDao.getNotesForLead(leadId)
    fun getTimelineForLead(leadId: Int): Flow<List<TimelineEvent>> = crmDao.getTimelineForLead(leadId)

    suspend fun getLeadById(id: Int): Lead? = crmDao.getLeadById(id)

    suspend fun insertLead(lead: Lead): Int {
        val id = crmDao.insertLead(lead).toInt()
        
        // Log "Lead Received / Created" activity event
        logEvent(
            leadId = id,
            title = "Lead Created",
            description = "Captured from ${lead.leadSource} under ${lead.assignedTeam}",
            iconType = "receive"
        )
        return id
    }

    suspend fun updateLeadStatus(lead: Lead, newStatus: String, author: String = "You") {
        val oldStatus = lead.status
        if (oldStatus == newStatus) return
        
        val updatedLead = lead.copy(status = newStatus)
        crmDao.updateLead(updatedLead)

        // Log status change activity event
        logEvent(
            leadId = lead.id,
            title = "Status Changed",
            description = "Status updated from '$oldStatus' to '$newStatus' by $author",
            iconType = "status"
        )
    }

    suspend fun updateLeadDetails(lead: Lead) {
        crmDao.updateLead(lead)
    }

    suspend fun deleteLead(lead: Lead) {
        crmDao.deleteLead(lead)
    }

    suspend fun addNote(leadId: Int, content: String, author: String = "You") {
        val note = LeadNote(leadId = leadId, content = content, author = author)
        crmDao.insertNote(note)

        // Log note addition activity event
        logEvent(
            leadId = leadId,
            title = "Private Note Added",
            description = "\"$content\" by $author",
            iconType = "note"
        )
    }

    suspend fun addCustomTimelineEvent(leadId: Int, title: String, description: String, iconType: String) {
        logEvent(leadId, title, description, iconType)
    }

    suspend fun insertTemplate(template: MessageTemplate) {
        crmDao.insertTemplate(template)
    }

    suspend fun deleteTemplate(template: MessageTemplate) {
        crmDao.deleteTemplate(template)
    }

    private suspend fun logEvent(leadId: Int, title: String, description: String, iconType: String) {
        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val event = TimelineEvent(
            leadId = leadId,
            title = title,
            description = description,
            iconType = iconType,
            timeString = timeString
        )
        crmDao.insertTimelineEvent(event)
    }

    // Prepopulate database if it's empty
    suspend fun prepopulateIfEmpty(context: Context) {
        val existingLeads = crmDao.getAllLeads().firstOrNull() ?: emptyList()
        if (existingLeads.isEmpty()) {
            val templates = listOf(
                MessageTemplate(
                    name = "SMM Pitch (Interested)",
                    category = "Interested",
                    content = "Hello {name}, thank you for your interest in our SMM services! We noticed you run a {business} business and want to scale your lead flow. Let's schedule a brief 15-minute call to discuss how our custom {source} marketing strategy can grow your bookings. Would tomorrow at 3 PM work for you?"
                ),
                MessageTemplate(
                    name = "Follow-up SMM Ad Response",
                    category = "Follow-up",
                    content = "Hey {name}, hope you're having a productive week! I'm following up regarding your inquiry about our marketing campaigns. Just wanted to see if you had any questions, or if you'd like a free audit of your social media ads? Let me know if you are free for a quick Zoom call."
                ),
                MessageTemplate(
                    name = "Not Interested Closing Template",
                    category = "Not Interested",
                    content = "Thank you for your time, {name}! If you ever need SMM, lead generation campaigns, or Meta/Instagram Ads scaling in the future, we would love to partner with you. Feel free to keep our search open and contact us anytime. All the best with {business}!"
                ),
                MessageTemplate(
                    name = "Default Greeting Campaign",
                    category = "New Lead",
                    content = "Hi {name}! Welcome to SMM Growth Agency. We received your request from {source} for {business}. One of our marketing specialists will call you shortly to discuss your custom proposal. Stay tuned!"
                )
            )

            for (t in templates) {
                crmDao.insertTemplate(t)
            }

            // High Quality leads mock list
            val mockLeads = listOf(
                Lead(
                    name = "Aarav Sharma",
                    phone = "919876543210",
                    businessType = "E-Commerce Fashion",
                    leadSource = "Meta Ads",
                    budget = 25000.0,
                    status = "New Lead",
                    assignedTeam = "Abhiraj Gupta",
                    adCampaign = "SMM High ROAS - Spring Wear",
                    priority = "High",
                    aiLeadScore = 92
                ),
                Lead(
                    name = "Vikram Malhotra",
                    phone = "919123456789",
                    businessType = "Luxury Real Estate",
                    leadSource = "LinkedIn Outreach",
                    budget = 60000.0,
                    status = "Replied",
                    assignedTeam = "Abhiraj Gupta",
                    adCampaign = "High-Net B2B Campaigns",
                    priority = "High",
                    aiLeadScore = 88
                ),
                Lead(
                    name = "Priya Patel",
                    phone = "919109876543",
                    businessType = "Dental Clinic Chain",
                    leadSource = "Meta Ads",
                    budget = 18000.0,
                    status = "Interested",
                    assignedTeam = "Sarah Connor",
                    adCampaign = "Local Dental Implants Inflow",
                    priority = "Medium",
                    aiLeadScore = 78
                ),
                Lead(
                    name = "Rohan Kapoor",
                    phone = "919567890123",
                    businessType = "D2C Gym Nutrition",
                    leadSource = "Organic Instagram",
                    budget = 35000.0,
                    status = "Meeting Scheduled",
                    assignedTeam = "John Doe",
                    adCampaign = "Reel Funnel Growth",
                    priority = "High",
                    aiLeadScore = 95
                ),
                Lead(
                    name = "Neha Gupta",
                    phone = "919654321098",
                    businessType = "Premium Beauty Salon",
                    leadSource = "Meta Ads",
                    budget = 12000.0,
                    status = "No Reply",
                    assignedTeam = "John Doe",
                    adCampaign = "Local Salon Promo",
                    priority = "Low",
                    aiLeadScore = 54
                ),
                Lead(
                    name = "Sanjay Singhal",
                    phone = "919123123123",
                    businessType = "Co-working Hubs",
                    leadSource = "Google Search",
                    budget = 45000.0,
                    status = "Closed",
                    assignedTeam = "Sarah Connor",
                    adCampaign = "Inbound Google Local Pack",
                    priority = "Medium",
                    aiLeadScore = 85
                ),
                Lead(
                    name = "Ananya Deshmukh",
                    phone = "919888877776",
                    businessType = "HR Tech SaaS Startup",
                    leadSource = "LinkedIn Outreach",
                    budget = 80000.0,
                    status = "Not Interested",
                    assignedTeam = "Sarah Connor",
                    adCampaign = "Outbound SDR Campaign",
                    priority = "Medium",
                    aiLeadScore = 40
                )
            )

            for (lead in mockLeads) {
                val insertedId = crmDao.insertLead(lead).toInt()
                
                // Add default timeline events and notes to make each feel completed
                if (lead.status == "Interested") {
                    logPrepopulatedEvent(insertedId, "Lead Captured", "System captured lead from FaceBook Lead Ads Form", "receive", 3 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "WhatsApp Pitch Sent", "Automated WhatsApp SMM Pitch template sent to Priya", "whatsapp", 2 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "Client Replied", "Priya expressed interest in booking dental clients", "status", 1 * 3600 * 1000)
                    crmDao.insertNote(LeadNote(leadId = insertedId, content = "Priya needs Facebook-centric visual campaign. Budget can go up with good leads.", author = "Sarah Connor"))
                } else if (lead.status == "Meeting Scheduled") {
                    logPrepopulatedEvent(insertedId, "Lead Captured", "Captured from SMM Instagram profile click", "receive", 12 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "Intro Call Successful", "Talked to Rohan. Likes our past fitness campaign case study.", "call", 6 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "Meeting Arranged", "strategy proposal zoom meeting scheduled", "status", 2 * 3600 * 1000)
                    crmDao.insertNote(LeadNote(leadId = insertedId, content = "Rohan wants to drive influencer Reels. Looking for ₹35,000 pilot package.", author = "Abhiraj Gupta"))
                } else if (lead.status == "Closed") {
                    logPrepopulatedEvent(insertedId, "First Lead Capture", "Google Search Local SEO call received", "call", 5 * 24 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "Proposal Presentation", "Presented SMM co-working package", "status", 3 * 24 * 3600 * 1000)
                    logPrepopulatedEvent(insertedId, "Contract Signed", "₹45,000 monthly SMM retainer closed!", "status", 1 * 24 * 3600 * 1000)
                    crmDao.insertNote(LeadNote(leadId = insertedId, content = "Setup fee paid. Campaign launches on 1st of next month.", author = "Sarah Connor"))
                } else {
                    logPrepopulatedEvent(insertedId, "Lead Captured", "Inbound lead registered under assigned representative", "receive", 1 * 3600 * 1000)
                }
            }
        }
    }

    private suspend fun logPrepopulatedEvent(leadId: Int, title: String, description: String, iconType: String, offsetAgo: Long) {
        val dateValue = Date(System.currentTimeMillis() - offsetAgo)
        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(dateValue)
        val event = TimelineEvent(
            leadId = leadId,
            title = title,
            description = description,
            iconType = iconType,
            timeString = timeString,
            timestamp = System.currentTimeMillis() - offsetAgo
        )
        crmDao.insertTimelineEvent(event)
    }
}
