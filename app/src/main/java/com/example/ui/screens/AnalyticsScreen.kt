package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import com.example.database.Lead
import com.example.ui.CrmViewModel

@Composable
fun AnalyticsScreen(viewModel: CrmViewModel, modifier: Modifier = Modifier) {
    val leadsList by viewModel.allLeads.collectAsStateWithLifecycle(emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Intelligence Reports",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Performance Diagnostics",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 1. Revenue Progression Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly SMM MRR Growth",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Closed retainers comparison over last 4 months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    MonthlyRevenueBarChart()
                }
            }
        }

        // 2. Conversion & Lead Sources Pie Wedge Split
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Attribution wedge Card (Pie chart)
                Card(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Channel Attribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ChannelDistributionPie(leadsList)
                    }
                }
            }
        }

        // 3. Team Member Productivity Index
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account Manager Conversion Rates",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Funnel conversion efficiency score",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TeamProductivityRow(name = "Abhiraj Gupta", activeLeads = 3, progressVal = 0.85f, percentText = "85%")
                    TeamProductivityRow(name = "Sarah Connor", activeLeads = 2, progressVal = 0.72f, percentText = "72%")
                    TeamProductivityRow(name = "John Doe", activeLeads = 2, progressVal = 0.50f, percentText = "50%")
                    TeamProductivityRow(name = "Alex Rivera", activeLeads = 0, progressVal = 0.15f, percentText = "15%")
                }
            }
        }
    }
}

@Composable
fun MonthlyRevenueBarChart() {
    val barHeights = listOf(0.42f, 0.65f, 0.78f, 0.95f)
    val months = listOf("Feb", "Mar", "Apr", "May (Live)")
    val revenueTexts = listOf("₹1.2L", "₹1.8L", "₹2.2L", "₹2.7L")

    val indigoBrush = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height

        val barCount = barHeights.size
        val availableBarArea = totalWidth * 0.82f
        val spaceBetweenBars = (totalWidth - availableBarArea) / (barCount + 1)
        val barWidth = availableBarArea / barCount

        var currentX = spaceBetweenBars

        for (i in 0 until barCount) {
            val h = barHeights[i] * (totalHeight - 50.dp.toPx())
            val yOffset = totalHeight - h - 24.dp.toPx()

            // Draw Bar
            drawRoundRect(
                brush = indigoBrush,
                topLeft = Offset(currentX, yOffset),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            currentX += barWidth + spaceBetweenBars
        }
    }

    // Label indicators beneath the canvas
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        months.forEachIndexed { i, m ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(m, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(revenueTexts[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ChannelDistributionPie(leads: List<Lead>) {
    // Group and calculate channels percentages
    val totalCount = leads.size.coerceAtLeast(1)
    val metaCount = leads.count { it.leadSource == "Meta Ads" }
    val linkedInCount = leads.count { it.leadSource == "LinkedIn Outreach" }
    val contentCount = leads.count { it.leadSource == "Organic Instagram" }
    val googleCount = leads.count { it.leadSource == "Google Search" }
    val referralCount = leads.count { it.leadSource == "Client Referral" }

    val wedges = listOf(
        PieWedge(Color(0xFF5A60FF), metaCount.toFloat() / totalCount, "Meta Ads"),
        PieWedge(Color(0xFF06B6D4), linkedInCount.toFloat() / totalCount, "LinkedIn"),
        PieWedge(Color(0xFF10B981), contentCount.toFloat() / totalCount, "Organic Instagram"),
        PieWedge(Color(0xFFF59E0B), googleCount.toFloat() / totalCount, "Google Search"),
        PieWedge(Color(0xFF94A3B8), referralCount.toFloat() / totalCount, "Referrals")
    ).filter { it.sweepRatio > 0f }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Render Pie wedger
        Canvas(
            modifier = Modifier
                .size(110.dp)
                .weight(1f)
        ) {
            var currentStartAngle = -90f
            
            if (wedges.isEmpty()) {
                // Empty fallback circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 14.dp.toPx())
                )
            } else {
                wedges.forEach { wedge ->
                    val sweep = wedge.sweepRatio * 360f
                    drawArc(
                        color = wedge.color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    currentStartAngle += sweep
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Render Legend list alongside
        Column(
            modifier = Modifier.weight(1.3f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (wedges.isEmpty()) {
                Text("No attribution data captured.", style = MaterialTheme.typography.labelMedium)
            } else {
                wedges.forEach { wedge ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(wedge.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${wedge.label}: ${(wedge.sweepRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

data class PieWedge(val color: Color, val sweepRatio: Float, val label: String)

@Composable
fun TeamProductivityRow(
    name: String,
    activeLeads: Int,
    progressVal: Float,
    percentText: String
) {
    val progressAnimate by animateFloatAsState(
        targetValue = progressVal,
        animationSpec = tween(durationMillis = 800)
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$name ($activeLeads Active)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = percentText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        LinearProgressIndicator(
            progress = { progressAnimate },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
