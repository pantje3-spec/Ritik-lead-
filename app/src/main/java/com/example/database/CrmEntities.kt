package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val businessType: String,
    val leadSource: String, // e.g. "Meta Ads", "LinkedIn", "Organic", "Google Search"
    val budget: Double,
    val budgetCurrency: String = "₹",
    val status: String, // "New Lead", "Contacted", "No Reply", "Replied", "Interested", "Meeting Scheduled", "Follow-up", "Closed", "Not Interested"
    val assignedTeam: String, // e.g., "Abhiraj Gupta", "Sarah Connor", "John Doe"
    val dateAdded: Long = System.currentTimeMillis(),
    val followUpDate: Long? = null,
    val adCampaign: String = "",
    val priority: String = "Medium", // High, Medium, Low
    val aiLeadScore: Int = 80 // 1-100 score
)

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val timeString: String, // e.g., "10:05 AM"
    val title: String,
    val description: String,
    val iconType: String // "receive", "whatsapp", "call", "status", "note", "system"
)

@Entity(tableName = "lead_notes")
data class LeadNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val author: String = "You"
)

@Entity(tableName = "message_templates")
data class MessageTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g., "Interested", "No Reply", "Not Interested"
    val content: String
)
