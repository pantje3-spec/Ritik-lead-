package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.Lead
import com.example.database.LeadNote
import com.example.database.TimelineEvent
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: CrmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isLightMode by viewModel.isLightMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = !isLightMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrmWorkspace(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CrmWorkspace(viewModel: CrmViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    val currentScreen by viewModel.selectedScreen.collectAsStateWithLifecycle()
    val selectedLead by viewModel.selectedLead.collectAsStateWithLifecycle()
    val pendingWhatsAppPopup by viewModel.pendingWhatsAppPopup.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGeneratingPitch.collectAsStateWithLifecycle()

    // Slide-panel lists
    val notes by viewModel.selectedLeadNotes.collectAsStateWithLifecycle()
    val timeline by viewModel.selectedLeadTimeline.collectAsStateWithLifecycle()

    // Compose Responsive detection
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        if (isTablet) {
            // Render High End Landscape Tablet with Persistent Sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                LeftSidebarContent(
                    selectedScreen = currentScreen,
                    onScreenSelected = { viewModel.selectedScreen.value = it },
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(245.dp)
                        .fillMaxHeight()
                )
                
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(modifier = Modifier.weight(1f)) {
                    CrmTopNavigationBar(
                        viewModel = viewModel,
                        toggleMenu = { /* Not applicable for tablet layouts as sidebar is fixed */ },
                        isTablet = true
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Selected Section Board
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenRouter(
                            selectedScreen = currentScreen,
                            viewModel = viewModel,
                            onLeadClick = { lead -> viewModel.selectedLead.value = lead }
                        )

                        // 1. Sleek Right Slide Over Details Sheet
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedLead != null,
                            enter = slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = spring(stiffness = 500f)
                            ) + fadeIn(),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(stiffness = 500f)
                            ) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(380.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("tablet_details_panel")
                        ) {
                            selectedLead?.let { lead ->
                                LeadDetailsSlidePanel(
                                    lead = lead,
                                    notes = notes,
                                    timeline = timeline,
                                    onDismiss = { viewModel.selectedLead.value = null },
                                    onAddNote = { content -> viewModel.addLeadNote(lead.id, content) },
                                    onStatusChange = { newStatus -> viewModel.changeLeadStatus(lead, newStatus) },
                                    onRunAiProposal = { viewModel.runGeminiCampaignPitch(lead) },
                                    onRunAiAudit = { viewModel.runGeminiLeadAudit(lead) },
                                    isAiProcessing = isAiGenerating
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Render Mobile Portrait layout using standard navigation Drawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(260.dp),
                        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        LeftSidebarContent(
                            selectedScreen = currentScreen,
                            onScreenSelected = { screen ->
                                viewModel.selectedScreen.value = screen
                                coroutineScope.launch { drawerState.close() }
                            },
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        CrmTopNavigationBar(
                            viewModel = viewModel,
                            toggleMenu = { coroutineScope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } },
                            isTablet = false
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ScreenRouter(
                            selectedScreen = currentScreen,
                            viewModel = viewModel,
                            onLeadClick = { lead -> viewModel.selectedLead.value = lead }
                        )

                        // Mobile Bottom modal overlay for Lead Details
                        AnimatedVisibility(
                            visible = selectedLead != null,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(stiffness = 500f)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = spring(stiffness = 500f)
                            ) + fadeOut(),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("mobile_details_panel")
                        ) {
                            selectedLead?.let { lead ->
                                LeadDetailsSlidePanel(
                                    lead = lead,
                                    notes = notes,
                                    timeline = timeline,
                                    onDismiss = { viewModel.selectedLead.value = null },
                                    onAddNote = { content -> viewModel.addLeadNote(lead.id, content) },
                                    onStatusChange = { newStatus -> viewModel.changeLeadStatus(lead, newStatus) },
                                    onRunAiProposal = { viewModel.runGeminiCampaignPitch(lead) },
                                    onRunAiAudit = { viewModel.runGeminiLeadAudit(lead) },
                                    isAiProcessing = isAiGenerating
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Customizable WhatsApp Template Preview Popup
        pendingWhatsAppPopup?.let { state ->
            WhatsAppComposerDialog(
                popupState = state,
                viewModel = viewModel,
                isAiProcessing = isAiGenerating,
                onDismiss = { viewModel.dismissWhatsAppPopup() },
                onLaunchWhatsApp = { editedText ->
                    val encodedText = URLEncoder.encode(editedText, "UTF-8")
                    val phoneStr = state.lead.phone
                    val waUrl = "https://api.whatsapp.com/send?phone=$phoneStr&text=$encodedText"
                    
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                        context.startActivity(browserIntent)
                        
                        // Register timeline event update
                        viewModel.addTimelineEvent(
                            leadId = state.lead.id,
                            title = "WhatsApp Action Sent",
                            description = "Dispatched customized outreach pitch via external WhatsApp intent",
                            iconType = "whatsapp"
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open WhatsApp link, trying direct copy.", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.dismissWhatsAppPopup()
                }
            )
        }
    }
}

@Composable
fun ScreenRouter(
    selectedScreen: SelectedScreen,
    viewModel: CrmViewModel,
    onLeadClick: (Lead) -> Unit
) {
    when (selectedScreen) {
        SelectedScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onLeadClick = onLeadClick)
        SelectedScreen.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
        SelectedScreen.WHATSAPP_TEMPLATES -> TemplatesScreen(viewModel = viewModel)
        SelectedScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
        // Pipeline stages use LeadsScreen with filter constraints handled reactively in ViewModel!
        else -> LeadsScreen(viewModel = viewModel, onLeadClick = onLeadClick)
    }
}

@Composable
fun LeftSidebarContent(
    selectedScreen: SelectedScreen,
    onScreenSelected: (SelectedScreen) -> Unit,
    viewModel: CrmViewModel,
    modifier: Modifier = Modifier
) {
    val totalLeads by viewModel.totalLeadsCount.collectAsStateWithLifecycle(0)
    val todayLeads by viewModel.todayLeadsCount.collectAsStateWithLifecycle(0)
    val unreadNotifs = viewModel.notifications.collectAsStateWithLifecycle(emptyList()).value.count { !it.isRead }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag("left_sidebar"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // SMM Brand Identifier Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp, top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "SMM CRM icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SMM Leads",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Growth Agency CRM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Menu Scroll List
            val menuItems = SelectedScreen.values()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(menuItems) { screen ->
                    val isSelected = selectedScreen == screen
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onScreenSelected(screen) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (screen) {
                                SelectedScreen.DASHBOARD -> Icons.Default.Dashboard
                                SelectedScreen.NEW_LEADS -> Icons.Default.FolderOpen
                                SelectedScreen.FOLLOW_UP -> Icons.Default.Timer
                                SelectedScreen.INTERESTED_LEADS -> Icons.Default.Favorite
                                SelectedScreen.REPLIED_LEADS -> Icons.Default.QuestionAnswer
                                SelectedScreen.CLOSED_CLIENTS -> Icons.Default.Verified
                                SelectedScreen.NOT_INTERESTED -> Icons.Default.Block
                                SelectedScreen.ANALYTICS -> Icons.Default.ShowChart
                                SelectedScreen.WHATSAPP_TEMPLATES -> Icons.Default.Sms
                                SelectedScreen.SETTINGS -> Icons.Default.Settings
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // SMM dynamic notifications or count badges next to listing
                        if (screen == SelectedScreen.DASHBOARD && todayLeads > 0) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+$todayLeads", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 9.sp)
                            }
                        } else if (screen == SelectedScreen.NEW_LEADS && totalLeads > 0) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$totalLeads", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Profile Footer containing Abhiraj's credentials from tambahan meta!
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Initial
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AG", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Abhiraj Gupta",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "abhirajgupta12p@gmail.com",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmTopNavigationBar(
    viewModel: CrmViewModel,
    toggleMenu: () -> Unit,
    isTablet: Boolean
) {
    var showNotifsDropdown by remember { mutableStateOf(false) }
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount = notifications.count { !it.isRead }

    val todayCount by viewModel.todayLeadsCount.collectAsStateWithLifecycle(0)

    val lightModeActive by viewModel.isLightMode.collectAsStateWithLifecycle()

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isTablet) {
                    IconButton(onClick = toggleMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Sleek Drawer trigger")
                    }
                }
                
                // Live Counter indicating inbound counts
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Today's Inflow: $todayCount leads",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        actions = {
            // Dark Mode Switch Icon clicks
            IconButton(onClick = { viewModel.isLightMode.value = !lightModeActive }) {
                Icon(
                    imageVector = if (lightModeActive) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Switch user layout visual representation"
                )
            }

            // Interactive Notifications drop down trigger
            Box {
                IconButton(onClick = { showNotifsDropdown = true }) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(containerColor = Color(0xFFEF4444)) {
                                    Text(unreadCount.toString(), color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Leads alerts center")
                    }
                }

                DropdownMenu(
                    expanded = showNotifsDropdown,
                    onDismissRequest = { showNotifsDropdown = false },
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "CRM Notifications Checklist",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Divider()

                        if (notifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No alerts active.")
                            }
                        } else {
                            notifications.forEach { notif ->
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(notif.timestamp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                            }
                                            Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.markNotificationAsRead(notif.id)
                                    }
                                )
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun LeadDetailsSlidePanel(
    lead: Lead,
    notes: List<LeadNote>,
    timeline: List<TimelineEvent>,
    onDismiss: () -> Unit,
    onAddNote: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onRunAiProposal: () -> Unit,
    onRunAiAudit: () -> Unit,
    isAiProcessing: Boolean
) {
    var newNoteText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Info, 1 = Notes, 2 = Logs

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Back icon / closing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Dismiss slides")
            }
            Text(
                text = "Prospect Profile",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            // AI Badge Info
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${lead.priority} priority", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Basic details overview card representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lead.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text("Niche: ${lead.businessType}", style = MaterialTheme.typography.bodyMedium)
                Text("Allocated Budget: ${lead.budgetCurrency}${String.format("%,.0f", lead.budget)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Representing Campaign: " + (lead.adCampaign.ifEmpty { "General Outreach" }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Magic Actions Segment
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, "Gemini core functions icon", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Social AI Smart Co-Pilot", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                if (isAiProcessing) {
                    Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Gemini is crunching data...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunAiAudit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Health Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onRunAiProposal,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Text("Pitch Proposal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs toggle line representation
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Attributes", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Private Notes", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (notes.isNotEmpty()) {
                        Badge { Text(notes.size.toString()) }
                    }
                }
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("SMM Timeline", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    // Quick Info summary Attributes list
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { InfoAttributeItem(label = "Platform Lead Origin", value = lead.leadSource) }
                        item { InfoAttributeItem(label = "Allocated Manager", value = lead.assignedTeam) }
                        item { InfoAttributeItem(label = "Telephone Reach", value = lead.phone) }
                        item { InfoAttributeItem(label = "AI Score Evaluated", value = "${lead.aiLeadScore}% Match") }
                        item {
                            val addDateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lead.dateAdded))
                            InfoAttributeItem(label = "System Entry Timestamp", value = addDateStr)
                        }
                    }
                }
                1 -> {
                    // Notes feed with quick editor input text
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                placeholder = { Text("Attach custom team memo...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (newNoteText.isNotEmpty()) {
                                        onAddNote(newNoteText)
                                        newNoteText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Add log note", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (notes.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No internal notes recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(notes, key = { it.id }) { n ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(n.author, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                val tStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(n.timestamp))
                                                Text(tStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(n.content, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Timeline Activity logs
                    if (timeline.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Zero timeline tracking events.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(timeline, key = { it.id }) { ev ->
                                TimelineCardRow(event = ev)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoAttributeItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun TimelineCardRow(event: TimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Dot indicator sphere
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = when (event.iconType) {
                            "receive" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                            "whatsapp" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "call" -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            "status" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (event.iconType) {
                        "receive" -> Icons.Default.CloudDownload
                        "whatsapp" -> Icons.Default.ChatBubble
                        "call" -> Icons.Default.PhoneCallback
                        "status" -> Icons.Default.Autorenew
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = when (event.iconType) {
                        "receive" -> Color(0xFF3B82F6)
                        "whatsapp" -> Color(0xFF10B981)
                        "call" -> Color(0xFF8B5CF6)
                        "status" -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            // Vertical timeline connecting line
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(event.timeString, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            Text(
                event.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun WhatsAppComposerDialog(
    popupState: WhatsAppPopupState,
    viewModel: CrmViewModel,
    isAiProcessing: Boolean,
    onDismiss: () -> Unit,
    onLaunchWhatsApp: (String) -> Unit
) {
    var editedMessage by remember(popupState.messageText) { mutableStateOf(popupState.messageText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF25D366), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, "WhatsApp action visual symbol", modifier = Modifier.size(14.dp), tint = Color.White)
                }
                Text("WhatsApp Outreach Panel", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Outbound Client: ${popupState.lead.name} (${popupState.lead.phone})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )

                // Status banner based template name
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Source Template: ${popupState.templateName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isAiProcessing) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Gemini is rewriting proposal draft...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = editedMessage,
                        onValueChange = { editedMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        maxLines = 15
                    )
                }

                // AI suggestions triggers
                TextButton(
                    onClick = { viewModel.runGeminiCampaignPitch(popupState.lead) },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isAiProcessing
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rewrite with Gemini SMM AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLaunchWhatsApp(editedMessage) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Text("Launch Chat", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
