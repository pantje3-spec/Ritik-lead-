package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiHelper
import com.example.ui.CrmViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: CrmViewModel, modifier: Modifier = Modifier) {
    val currentAdSpend by viewModel.totalAdSpend.collectAsStateWithLifecycle()
    var spendInput by remember { mutableStateOf(currentAdSpend.toInt().toString()) }

    var localModeToggle by remember { mutableStateOf(!viewModel.isLightMode.value) }

    val keyLoaded = GeminiHelper.isKeyConfigured

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "System Administation",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "CRM Settings & Keys",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // 1. Theme and visual options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Interface Theme Preference", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark High-Contrast Mode", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = localModeToggle,
                            onCheckedChange = {
                                localModeToggle = it
                                viewModel.isLightMode.value = !it
                            }
                        )
                    }
                }
            }

            // 2. Adjust budget metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Agency Ads Spend Parameters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Customize the total Meta/LinkedIn monthly ads-spend to calculate live Cost Per Lead (CPL) stats in the Dashboard:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = spendInput,
                            onValueChange = { spendInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            prefix = { Text("₹") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                spendInput.toDoubleOrNull()?.let {
                                    viewModel.totalAdSpend.value = it
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Recompute")
                        }
                    }
                }
            }

            // 3. Gemini Key diagnostics (Mandatory skill warnings)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Gemini AI Core Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(
                                color = if (keyLoaded) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (keyLoaded) Color(0xFF10B981) else Color(0xFFEF4444), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (keyLoaded) "ACTIVE KEY" else "OFFLINE FALLBACK",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = if (keyLoaded) "Gemini is connected. AI scores and proposals will be generated in real-time."
                            else "Gemini API Key missing or default. Simulated heuristic backups are active.",
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Security Caveat from Skill instructions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                        Text(
                            text = "Caution: This is an AI studio diagnostic interface. Storing API Keys straight inside BuildConfig are secure for sandbox modeling but should be guarded via Firebase App Check for final play store release.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
