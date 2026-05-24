package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmDao {
    @Query("SELECT * FROM leads ORDER BY dateAdded DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getLeadById(id: Int): Lead?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    // Notes
    @Query("SELECT * FROM lead_notes WHERE leadId = :leadId ORDER BY timestamp DESC")
    fun getNotesForLead(leadId: Int): Flow<List<LeadNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: LeadNote): Long

    @Delete
    suspend fun deleteNote(note: LeadNote)

    // Timeline
    @Query("SELECT * FROM timeline_events WHERE leadId = :leadId ORDER BY timestamp DESC")
    fun getTimelineForLead(leadId: Int): Flow<List<TimelineEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvent(event: TimelineEvent): Long

    // Templates
    @Query("SELECT * FROM message_templates")
    fun getAllTemplates(): Flow<List<MessageTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: MessageTemplate)
}
