package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.Lead
import com.example.ui.AdCampaignPerformance
import com.example.ui.CrmViewModel

@Composable
fun DashboardScreen(
    viewModel: CrmViewModel,
    modifier: Modifier = Modifier,
    onLeadClick: (Lead) -> Unit
) {
    // Collect reactive statistics
    val totalLeads = viewModel.totalLeadsCount.collectAsStateWithLifecycle(0).value
    val todayLeads = viewModel.todayLeadsCount.collectAsStateWithLifecycle(0).value
    val interestedCount = viewModel.interestedLeadsCount.collectAsStateWithLifecycle(0).value
    val followUpCount = viewModel.followUpLeadsCount.collectAsStateWithLifecycle(0).value
    val closedDeals = viewModel.closedRetainersCount.collectAsStateWithLifecycle(0).value
    val totalRevenue = viewModel.totalRevenueGenerated.collectAsStateWithLifecycle(0.0).value
    val cpl = viewModel.costPerLead.collectAsStateWithLifecycle(0.0).value
    val leadsList = viewModel.allLeads.collectAsStateWithLifecycle(emptyList()).value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Headline
        item {
            Column {
                Text(
                    text = "Social Growth Analytics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Active Agency Command Center",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }

        // 1. Grid of KPI summary cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Closed Revenue",
                        value = "₹" + String.format("%,.0f", totalRevenue),
                        icon = Icons.Default.AttachMoney,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total SMM Leads",
                        value = totalLeads.toString(),
                        icon = Icons.Default.TrendingUp,
                        gradientColors = listOf(Color(0xFF5A60FF), Color(0xFF3B82F6)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Interested / Active",
                        value = interestedCount.toString(),
                        icon = Icons.Default.Recommend,
                        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Average CPL",
                        value = "₹" + String.format("%.1f", cpl),
                        icon = Icons.Default.PieChart,
                        gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF0891B2)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Follow-ups Due",
                        value = followUpCount.toString(),
                        icon = Icons.Default.HourglassBottom,
                        gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Today's Inflow",
                        value = "+$todayLeads",
                        icon = Icons.Default.FlashOn,
                        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Active Ads Tracker list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meta Campaigns Attribution",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Live Syncing",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    viewModel.adCampaigns.forEach { campaign ->
                        CampaignPerformanceRow(campaign = campaign)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 3. Highlighted Leads Inflow (Quick View List)
        item {
            Text(
                text = "Recent CRM Activity",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (leadsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No SMM Leads Captured Yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val previewLeads = leadsList.take(4)
            items(previewLeads, key = { it.id }) { lead ->
                LeadQuickRow(lead = lead, onClick = { onLeadClick(lead) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* interaction splash */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp),
                tint = Color.White.copy(alpha = 0.15f)
            )

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }
    }
}

@Composable
fun CampaignPerformanceRow(campaign: AdCampaignPerformance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (campaign.status == "Active") Color(0xFF10B981).copy(alpha = 0.15f)
                    else Color.Gray.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (campaign.status == "Active") Icons.Default.Campaign else Icons.Default.Pause,
                contentDescription = null,
                tint = if (campaign.status == "Active") Color(0xFF10B981) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = campaign.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Spend: ₹${String.format("%,.0f", campaign.spend)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Leads: ${campaign.leads}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${campaign.cpl.toInt()} CPL",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${campaign.roi}x ROI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun LeadQuickRow(lead: Lead, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatars initials
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lead.name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lead.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${lead.businessType} • ${lead.leadSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Lead score circle indicator
            Box(
                modifier = Modifier
                    .background(
                        color = when {
                            lead.aiLeadScore >= 80 -> Color(0xFF10B981).copy(alpha = 0.15f)
                            lead.aiLeadScore >= 60 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${lead.aiLeadScore} AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
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
}
