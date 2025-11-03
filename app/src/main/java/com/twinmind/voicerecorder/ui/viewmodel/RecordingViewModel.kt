package com.twinmind.voicerecorder.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.data.local.entity.Meeting
import com.twinmind.voicerecorder.data.local.entity.MeetingStatus
import com.twinmind.voicerecorder.data.local.entity.RecordingState
import com.twinmind.voicerecorder.data.repository.MeetingRepository
import com.twinmind.voicerecorder.service.RecordingService
import com.twinmind.voicerecorder.utils.StorageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    application: Application,
    private val meetingRepository: MeetingRepository
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private var recordingService: RecordingService? = null
    private var isBound = false

    private val _currentMeeting = MutableStateFlow<Meeting?>(null)
    val currentMeeting: StateFlow<Meeting?> = _currentMeeting.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingBinder
            recordingService = binder.getService()
            isBound = true

            // Observe recording state from service
            viewModelScope.launch {
                recordingService?.recordingState?.collect { state ->
                    _recordingState.value = state
                }
            }

            // Observe recording duration from service
            viewModelScope.launch {
                recordingService?.recordingDuration?.collect { duration ->
                    _recordingDuration.value = duration
                }
            }

            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            isBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    companion object {
        private const val TAG = "RecordingViewModel"
    }

    init {
        // Load active meeting if any
        viewModelScope.launch {
            val activeMeeting = meetingRepository.getActiveMeeting()
            _currentMeeting.value = activeMeeting

            activeMeeting?.let {
                bindToService()
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                // Check storage
                if (!StorageUtils.hasEnoughStorage(context)) {
                    _errorMessage.value = "Not enough storage space. Please free up some space."
                    return@launch
                }

                // Create new meeting
                val meeting = Meeting(
                    startTime = System.currentTimeMillis(),
                    status = MeetingStatus.RECORDING,
                    recordingState = RecordingState.RECORDING
                )

                val meetingId = meetingRepository.createMeeting(meeting)
                _currentMeeting.value = meeting.copy(id = meetingId)

                // Start recording service
                val intent = Intent(context, RecordingService::class.java).apply {
                    putExtra("meeting_id", meetingId)
                }
                context.startForegroundService(intent)

                // Bind to service
                bindToService()

                Log.d(TAG, "Started recording for meeting $meetingId")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                _errorMessage.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                recordingService?.stopRecordingAndFinalize()
                unbindFromService()

                _currentMeeting.value = null
                _recordingState.value = RecordingState.STOPPED
                _recordingDuration.value = 0L

                Log.d(TAG, "Stopped recording")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                _errorMessage.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun bindToService() {
        if (!isBound) {
            val intent = Intent(context, RecordingService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            recordingService = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindFromService()
    }
}
