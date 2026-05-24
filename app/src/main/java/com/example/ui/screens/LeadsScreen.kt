package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Lead
import com.example.ui.CrmViewModel
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: CrmViewModel,
    modifier: Modifier = Modifier,
    onLeadClick: (Lead) -> Unit
) {
    val context = LocalContext.current
    val leadsList by viewModel.filteredLeads.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val activePriorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    val activeSourceFilter by viewModel.leadSourceFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("leads_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search & Sort Filtering Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search client name, business...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { showFilterSheet = !showFilterSheet },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (showFilterSheet || activeSourceFilter != null || activePriorityFilter != null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (showFilterSheet) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Filter menu",
                        tint = if (showFilterSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expanding Advanced Filter Sheet Panel inline
            AnimatedVisibility(
                visible = showFilterSheet,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterAdjustmentRow(
                    viewModel = viewModel,
                    activeSortOrder = activeSortOrder,
                    activePriorityFilter = activePriorityFilter,
                    activeSourceFilter = activeSourceFilter
                )
            }

            // Headline showing current total results
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${leadsList.size} Qualified Prospects Found",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activePriorityFilter != null || activeSourceFilter != null || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Reset Constraints",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable {
                            viewModel.priorityFilter.value = null
                            viewModel.leadSourceFilter.value = null
                            viewModel.searchQuery.value = ""
                        }
                    )
                }
            }

            // Leads card dynamic responsive grid
            if (leadsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No prospects align with filters.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add new leads using the wizard at the bottom corner.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(leadsList, key = { it.id }) { lead ->
                        LeadCard(
                            lead = lead,
                            onClick = { onLeadClick(lead) },
                            onStatusChange = { newStatus ->
                                viewModel.changeLeadStatus(lead, newStatus)
                            },
                            onWhatsAppClick = {
                                viewModel.runGeminiCampaignPitch(lead)
                                viewModel.logWhatsAppOutreachSent(lead)
                            },
                            onCallClick = {
                                val telIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                try {
                                    context.startActivity(telIntent)
                                    viewModel.addTimelineEvent(
                                        lead.id,
                                        "Call Dialed",
                                        "Initiated a direct phone call to ${lead.name}",
                                        "call"
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Call dialer not available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Add Quick Lead floating trigger
        LargeFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("quick_add_lead_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Lead", modifier = Modifier.size(32.dp))
        }

        if (showAddDialog) {
            QuickAddLeadDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterAdjustmentRow(
    viewModel: CrmViewModel,
    activeSortOrder: String,
    activePriorityFilter: String?,
    activeSourceFilter: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Priority Tag Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Priority:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val priorities = listOf("High", "Medium", "Low")
                    priorities.forEach { priority ->
                        val isSelected = activePriorityFilter == priority
                        InputChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.priorityFilter.value = if (isSelected) null else priority
                            },
                            label = { Text(priority, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Lead Source Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Source:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.leadSources.forEach { source ->
                        val isSelected = activeSourceFilter == source
                        InputChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.leadSourceFilter.value = if (isSelected) null else source
                            },
                            label = { Text(source, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Sort Selector Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort Order:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(76.dp))
                val sortingOptions = listOf("Date Added (Newest)", "Budget (High to Low)", "AI Score (High to Low)", "Priority")
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(activeSortOrder, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sortingOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.sortOrder.value = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadCard(
    lead: Lead,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit,
    viewModel: CrmViewModel
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val statuses = listOf("New Lead", "Contacted", "No Reply", "Replied", "Interested", "Meeting Scheduled", "Follow-up", "Closed", "Not Interested")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lead_card_id_${lead.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // First Row: Profile Logo and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Circle Initials
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lead.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = lead.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Budget text with Indian currency styling
                            Text(
                                text = "${lead.budgetCurrency}${String.format("%,.0f", lead.budget)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = lead.businessType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // AI score
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                lead.aiLeadScore >= 80 -> Color(0xFF10B981).copy(alpha = 0.12f)
                                lead.aiLeadScore >= 60 -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                else -> Color(0xFFEF4444).copy(alpha = 0.12f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${lead.aiLeadScore}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    lead.aiLeadScore >= 80 -> Color(0xFF10B981)
                                    lead.aiLeadScore >= 60 -> Color(0xFFD97706)
                                    else -> Color(0xFFEF4444)
                                }
                            )
                        )
                        Text(
                            text = "AI Quality",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = when {
                                    lead.aiLeadScore >= 80 -> Color(0xFF10B981)
                                    lead.aiLeadScore >= 60 -> Color(0xFFD97706)
                                    else -> Color(0xFFEF4444)
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta Info Grid Lines
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Source
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lead.leadSource, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Assigned Person
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lead.assignedTeam, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Priority representation with colored dot
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (lead.priority) {
                                    "High" -> Color(0xFFEF4444)
                                    "Medium" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                },
                                shape = CircleShape
                            )
                    )
                    Text("${lead.priority} Priority", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Date representation
                val dateString = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(lead.dateAdded))
                Text(
                    text = "Added $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pipeline stage action triggers & Quick contact triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Status selector dropdown button
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (lead.status) {
                                "Closed" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "Interested" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                "Not Interested" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                "No Reply" -> Color.Gray.copy(alpha = 0.15f)
                                "Meeting Scheduled" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        elevation = null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .background(
                                        color = when (lead.status) {
                                            "Closed" -> Color(0xFF10B981)
                                            "Interested", "Meeting Scheduled" -> Color(0xFFF59E0B)
                                            "Not Interested" -> Color(0xFFEF4444)
                                            "No Reply" -> Color.Gray
                                            "Follow-up" -> Color(0xFFEC4899)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = lead.status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (lead.status) {
                                    "Closed" -> Color(0xFF047857)
                                    "Interested", "Meeting Scheduled" -> Color(0xFFD97706)
                                    "Not Interested" -> Color(0xFFB91C1C)
                                    "No Reply" -> Color.DarkGray
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        statuses.forEach { st ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = when (st) {
                                                    "Closed" -> Color(0xFF10B981)
                                                    "Interested", "Meeting Scheduled" -> Color(0xFFF59E0B)
                                                    "Not Interested" -> Color(0xFFEF4444)
                                                    "No Reply" -> Color.Gray
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                },
                                text = { Text(st, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    onStatusChange(st)
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }

                // 2. Dial Call button
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Contact phone dialer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. WhatsApp Auto-Pitch magic button
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .background(
                            Color(0xFF25D366).copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .size(38.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, // Magic AI template trigger representation
                        contentDescription = "WhatsApp smart pipeline templates",
                        tint = Color(0xFF1E7E34),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. Notes Drawer button
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .size(38.dp)
                ) {
                    Icon(
                        Icons.Default.NoteAdd,
                        contentDescription = "Inspect client history timeline & notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddLeadDialog(
    viewModel: CrmViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(viewModel.leadSources.first()) }
    var selectedTeam by remember { mutableStateOf(viewModel.teamMembers.first()) }
    var priority by remember { mutableStateOf("Medium") }
    var adCampaign by remember { mutableStateOf("") }

    var expandedSource by remember { mutableStateOf(false) }
    var expandedTeam by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Register New Prospect", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = businessType,
                    onValueChange = { businessType = it },
                    label = { Text("Business / Industry Type (e.g. Gym, Salon)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Budget (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Priority
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = priority,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedPriority = true }) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = expandedPriority, onDismissRequest = { expandedPriority = false }) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                DropdownMenuItem(text = { Text(p) }, onClick = {
                                    priority = p
                                    expandedPriority = false
                                })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = adCampaign,
                    onValueChange = { adCampaign = it },
                    label = { Text("Meta Ad Campaign (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Source Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSource,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attr. Lead Source") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedSource = true }) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(expanded = expandedSource, onDismissRequest = { expandedSource = false }) {
                        viewModel.leadSources.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                selectedSource = s
                                expandedSource = false
                            })
                        }
                    }
                }

                // Assigned Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedTeam,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign Manager") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedTeam = true }) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(expanded = expandedTeam, onDismissRequest = { expandedTeam = false }) {
                        viewModel.teamMembers.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = {
                                selectedTeam = m
                                expandedTeam = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && phone.isNotEmpty() && businessType.isNotEmpty() && budget.isNotEmpty()) {
                        viewModel.addNewLead(
                            name = name,
                            phone = phone,
                            businessType = businessType,
                            leadSource = selectedSource,
                            budget = budget.toDoubleOrNull() ?: 15000.0,
                            assignedTeam = selectedTeam,
                            campaign = adCampaign,
                            priority = priority
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Pre-qualify & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


