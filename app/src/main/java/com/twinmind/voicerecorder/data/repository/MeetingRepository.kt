package com.twinmind.voicerecorder.data.repository

import com.twinmind.voicerecorder.data.local.dao.AudioChunkDao
import com.twinmind.voicerecorder.data.local.dao.MeetingDao
import com.twinmind.voicerecorder.data.local.dao.SummaryDao
import com.twinmind.voicerecorder.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val audioChunkDao: AudioChunkDao,
    private val summaryDao: SummaryDao
) {

    // Meeting operations
    suspend fun createMeeting(meeting: Meeting): Long {
        return meetingDao.insert(meeting)
    }

    suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.update(meeting)
    }

    suspend fun getMeetingById(meetingId: Long): Meeting? {
        return meetingDao.getMeetingById(meetingId)
    }

    fun getMeetingByIdFlow(meetingId: Long): Flow<Meeting?> {
        return meetingDao.getMeetingByIdFlow(meetingId)
    }

    fun getAllMeetings(): Flow<List<Meeting>> {
        return meetingDao.getAllMeetings()
    }

    suspend fun getActiveMeeting(): Meeting? {
        return meetingDao.getActiveMeeting()
    }

    suspend fun updateMeetingStatus(meetingId: Long, status: MeetingStatus) {
        meetingDao.updateMeetingStatus(meetingId, status)
    }

    suspend fun updateRecordingState(
        meetingId: Long,
        state: RecordingState,
        pauseReason: String? = null
    ) {
        meetingDao.updateRecordingState(meetingId, state, pauseReason)
    }

    suspend fun finalizeMeeting(meetingId: Long, endTime: Long, duration: Long) {
        meetingDao.finalizeMeeting(meetingId, endTime, duration, MeetingStatus.STOPPED)
    }

    suspend fun updateTranscriptStatus(meetingId: Long, status: TranscriptStatus) {
        meetingDao.updateTranscriptStatus(meetingId, status)
    }

    suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus) {
        meetingDao.updateSummaryStatus(meetingId, status)
    }

    suspend fun updateTotalChunks(meetingId: Long, totalChunks: Int) {
        meetingDao.updateTotalChunks(meetingId, totalChunks)
    }

    suspend fun updateTranscribedChunks(meetingId: Long, transcribedChunks: Int) {
        meetingDao.updateTranscribedChunks(meetingId, transcribedChunks)
    }

    // Audio chunk operations
    suspend fun createAudioChunk(audioChunk: AudioChunk): Long {
        return audioChunkDao.insert(audioChunk)
    }

    suspend fun updateAudioChunk(audioChunk: AudioChunk) {
        audioChunkDao.update(audioChunk)
    }

    suspend fun getChunksByMeetingId(meetingId: Long): List<AudioChunk> {
        return audioChunkDao.getChunksByMeetingId(meetingId)
    }

    fun getChunksByMeetingIdFlow(meetingId: Long): Flow<List<AudioChunk>> {
        return audioChunkDao.getChunksByMeetingIdFlow(meetingId)
    }

    suspend fun getChunksByTranscriptStatus(
        meetingId: Long,
        status: TranscriptStatus
    ): List<AudioChunk> {
        return audioChunkDao.getChunksByTranscriptStatus(meetingId, status)
    }

    suspend fun updateChunkTranscript(
        chunkId: Long,
        status: TranscriptStatus,
        transcriptText: String?
    ) {
        audioChunkDao.updateTranscript(chunkId, status, transcriptText)
    }

    suspend fun updateChunkTranscriptError(
        chunkId: Long,
        status: TranscriptStatus,
        errorMessage: String
    ) {
        audioChunkDao.updateTranscriptError(chunkId, status, errorMessage)
    }

    suspend fun getChunkCount(meetingId: Long): Int {
        return audioChunkDao.getChunkCount(meetingId)
    }

    suspend fun getTranscribedChunkCount(meetingId: Long): Int {
        return audioChunkDao.getTranscribedChunkCount(meetingId)
    }

    suspend fun getLatestChunk(meetingId: Long): AudioChunk? {
        return audioChunkDao.getLatestChunk(meetingId)
    }

    // Summary operations
    suspend fun createSummary(summary: Summary): Long {
        return summaryDao.insert(summary)
    }

    suspend fun updateSummary(summary: Summary) {
        summaryDao.update(summary)
    }

    suspend fun getSummaryByMeetingId(meetingId: Long): Summary? {
        return summaryDao.getSummaryByMeetingId(meetingId)
    }

    fun getSummaryByMeetingIdFlow(meetingId: Long): Flow<Summary?> {
        return summaryDao.getSummaryByMeetingIdFlow(meetingId)
    }

    suspend fun updateSummaryContent(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        status: SummaryStatus = SummaryStatus.COMPLETED
    ) {
        summaryDao.updateSummaryContent(meetingId, title, summary, actionItems, keyPoints, status)
    }

    suspend fun updateSummaryStatusOnly(
        meetingId: Long,
        status: SummaryStatus,
        errorMessage: String? = null
    ) {
        summaryDao.updateSummaryStatus(meetingId, status, errorMessage)
    }

    // Combined operations
    suspend fun getFullMeetingData(meetingId: Long): MeetingData? {
        val meeting = getMeetingById(meetingId) ?: return null
        val chunks = getChunksByMeetingId(meetingId)
        val summary = getSummaryByMeetingId(meetingId)
        return MeetingData(meeting, chunks, summary)
    }

    data class MeetingData(
        val meeting: Meeting,
        val chunks: List<AudioChunk>,
        val summary: Summary?
    )
}
