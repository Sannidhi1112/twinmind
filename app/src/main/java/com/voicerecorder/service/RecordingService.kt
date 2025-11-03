package com.voicerecorder.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.voicerecorder.R
import com.voicerecorder.data.local.entity.*
import com.voicerecorder.data.repository.MeetingRepository
import com.voicerecorder.utils.AudioUtils
import com.voicerecorder.utils.StorageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var meetingRepository: MeetingRepository

    @Inject
    lateinit var audioManager: AudioManager

    private val binder = RecordingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaRecorder: MediaRecorder? = null
    private var currentMeetingId: Long = -1
    private var currentChunkNumber = 0
    private var currentChunkStartTime: Long = 0
    private var recordingStartTime: Long = 0
    private var pausedDuration: Long = 0
    private var lastPauseTime: Long = 0

    private var chunkRecordingJob: Job? = null
    private var timerJob: Job? = null
    private var silenceDetectionJob: Job? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private var audioFocusRequest: AudioFocusRequest? = null
    private var isAudioFocusGranted = false

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    handlePhoneStateChange(state)
                }
            }
        }
    }

    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Headphones unplugged - continue recording but notify
                showNotification("Microphone source changed - Headphones disconnected")
            }
        }
    }

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val ACTION_STOP = "com.voicerecorder.STOP_RECORDING"
        private const val ACTION_PAUSE = "com.voicerecorder.PAUSE_RECORDING"
        private const val ACTION_RESUME = "com.voicerecorder.RESUME_RECORDING"
    }

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceivers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecordingAndFinalize()
            ACTION_PAUSE -> pauseRecording(RecordingState.PAUSED_AUDIO_FOCUS_LOSS, "User paused")
            ACTION_RESUME -> resumeRecording()
            else -> {
                val meetingId = intent?.getLongExtra("meeting_id", -1L) ?: -1L
                if (meetingId != -1L) {
                    startRecording(meetingId)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
        cleanupResources()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording status and controls"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerReceivers() {
        // Phone state receiver
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val phoneFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            registerReceiver(phoneStateReceiver, phoneFilter)
        }

        // Audio becoming noisy receiver (headphones unplugged)
        val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(audioBecomingNoisyReceiver, noisyFilter)
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Phone state receiver not registered", e)
        }

        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Audio noisy receiver not registered", e)
        }
    }

    fun startRecording(meetingId: Long) {
        serviceScope.launch {
            try {
                // Check storage
                if (!StorageUtils.hasEnoughStorage()) {
                    showNotification("Recording stopped - Low storage")
                    stopSelf()
                    return@launch
                }

                // Request audio focus
                if (!requestAudioFocus()) {
                    showNotification("Cannot start recording - Audio focus unavailable")
                    return@launch
                }

                currentMeetingId = meetingId
                currentChunkNumber = 0
                recordingStartTime = System.currentTimeMillis()
                pausedDuration = 0

                // Update meeting state
                meetingRepository.updateRecordingState(
                    meetingId,
                    RecordingState.RECORDING,
                    null
                )

                _recordingState.value = RecordingState.RECORDING

                // Start foreground service
                startForeground()

                // Start recording timer
                startRecordingTimer()

                // Start first chunk
                startRecordingChunk()

                Log.d(TAG, "Recording started for meeting $meetingId")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                showNotification("Error starting recording: ${e.message}")
                stopSelf()
            }
        }
    }

    private fun startForeground() {
        val notification = createNotification("Recording...", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun startRecordingChunk() {
        withContext(Dispatchers.IO) {
            try {
                // Check storage before each chunk
                if (!StorageUtils.hasEnoughStorage()) {
                    withContext(Dispatchers.Main) {
                        stopRecordingAndFinalize()
                        showNotification("Recording stopped - Low storage")
                    }
                    return@withContext
                }

                val chunkFile = StorageUtils.getAudioChunkFile(
                    this@RecordingService,
                    currentMeetingId,
                    currentChunkNumber
                )

                currentChunkStartTime = System.currentTimeMillis()

                // Initialize MediaRecorder
                mediaRecorder = createMediaRecorder(chunkFile)
                mediaRecorder?.start()

                // Create audio chunk entry in database
                meetingRepository.createAudioChunk(
                    AudioChunk(
                        meetingId = currentMeetingId,
                        chunkNumber = currentChunkNumber,
                        filePath = chunkFile.absolutePath,
                        duration = AudioUtils.CHUNK_DURATION_MS,
                        fileSize = 0,
                        status = ChunkStatus.RECORDING,
                        startTime = currentChunkStartTime,
                        endTime = currentChunkStartTime + AudioUtils.CHUNK_DURATION_MS
                    )
                )

                // Schedule next chunk
                scheduleNextChunk()

                // Start silence detection
                startSilenceDetection()

                Log.d(TAG, "Started recording chunk $currentChunkNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting chunk", e)
                withContext(Dispatchers.Main) {
                    showNotification("Error recording: ${e.message}")
                }
            }
        }
    }

    private fun createMediaRecorder(outputFile: File): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(AudioUtils.getAudioSource())
            setOutputFormat(AudioUtils.getOutputFormat())
            setAudioEncoder(AudioUtils.getAudioEncoder())
            setAudioSamplingRate(AudioUtils.SAMPLE_RATE)
            setAudioEncodingBitRate(AudioUtils.BIT_RATE)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
    }

    private fun scheduleNextChunk() {
        chunkRecordingJob?.cancel()
        chunkRecordingJob = serviceScope.launch {
            delay(AudioUtils.CHUNK_DURATION_MS)

            if (_recordingState.value == RecordingState.RECORDING) {
                stopCurrentChunk()
                currentChunkNumber++
                startRecordingChunk()
            }
        }
    }

    private suspend fun stopCurrentChunk() {
        withContext(Dispatchers.IO) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                // Update chunk in database
                val chunks = meetingRepository.getChunksByMeetingId(currentMeetingId)
                val currentChunk = chunks.find { it.chunkNumber == currentChunkNumber }

                currentChunk?.let { chunk ->
                    val chunkFile = File(chunk.filePath)
                    val fileSize = StorageUtils.getFileSize(chunkFile)
                    val actualDuration = System.currentTimeMillis() - currentChunkStartTime

                    meetingRepository.updateAudioChunk(
                        chunk.copy(
                            status = ChunkStatus.RECORDED,
                            fileSize = fileSize,
                            duration = actualDuration,
                            endTime = System.currentTimeMillis()
                        )
                    )

                    // Update meeting total chunks
                    meetingRepository.updateTotalChunks(currentMeetingId, currentChunkNumber + 1)

                    // Trigger transcription for this chunk
                    triggerChunkTranscription(chunk.id)
                }

                Log.d(TAG, "Stopped chunk $currentChunkNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping chunk", e)
            }
        }
    }

    private fun triggerChunkTranscription(chunkId: Long) {
        // This will be handled by WorkManager
        val workManager = androidx.work.WorkManager.getInstance(this)
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(
                androidx.work.workDataOf(
                    "chunk_id" to chunkId,
                    "meeting_id" to currentMeetingId
                )
            )
            .build()

        workManager.enqueue(workRequest)
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (_recordingState.value == RecordingState.RECORDING) {
                    val duration = System.currentTimeMillis() - recordingStartTime - pausedDuration
                    _recordingDuration.value = duration
                    updateNotification("Recording - ${AudioUtils.formatDuration(duration)}")
                }
            }
        }
    }

    private fun startSilenceDetection() {
        silenceDetectionJob?.cancel()
        silenceDetectionJob = serviceScope.launch {
            delay(AudioUtils.SILENCE_DETECTION_DURATION_MS)

            // Check if audio amplitude is below threshold
            val maxAmplitude = try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (e: Exception) {
                0
            }

            if (maxAmplitude < AudioUtils.SILENCE_THRESHOLD) {
                showNotification("No audio detected - Check microphone")
            }
        }
    }

    private fun handlePhoneStateChange(state: String?) {
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Pause recording during call
                if (_recordingState.value == RecordingState.RECORDING) {
                    pauseRecording(RecordingState.PAUSED_PHONE_CALL, "Phone call")
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Resume recording after call
                if (_recordingState.value == RecordingState.PAUSED_PHONE_CALL) {
                    resumeRecording()
                }
            }
        }
    }

    private fun pauseRecording(state: RecordingState, reason: String) {
        serviceScope.launch {
            try {
                if (_recordingState.value != RecordingState.RECORDING) return@launch

                // Stop current chunk
                stopCurrentChunk()

                // Update state
                _recordingState.value = state
                lastPauseTime = System.currentTimeMillis()

                // Update meeting
                meetingRepository.updateRecordingState(currentMeetingId, state, reason)
                meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.PAUSED)

                // Update notification
                val statusMessage = when (state) {
                    RecordingState.PAUSED_PHONE_CALL -> "Paused - Phone call"
                    RecordingState.PAUSED_AUDIO_FOCUS_LOSS -> "Paused - Audio focus lost"
                    RecordingState.PAUSED_NO_AUDIO -> "Paused - No audio detected"
                    else -> "Paused"
                }
                showNotification(statusMessage)

                Log.d(TAG, "Recording paused: $reason")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    private fun resumeRecording() {
        serviceScope.launch {
            try {
                if (_recordingState.value == RecordingState.RECORDING) return@launch

                // Update paused duration
                if (lastPauseTime > 0) {
                    pausedDuration += System.currentTimeMillis() - lastPauseTime
                    lastPauseTime = 0
                }

                // Update state
                _recordingState.value = RecordingState.RECORDING

                // Update meeting
                meetingRepository.updateRecordingState(currentMeetingId, RecordingState.RECORDING)
                meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.RECORDING)

                // Start next chunk
                currentChunkNumber++
                startRecordingChunk()

                showNotification("Recording resumed")

                Log.d(TAG, "Recording resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    fun stopRecordingAndFinalize() {
        serviceScope.launch {
            try {
                // Stop current chunk
                if (mediaRecorder != null) {
                    stopCurrentChunk()
                }

                // Calculate final duration
                val endTime = System.currentTimeMillis()
                val duration = endTime - recordingStartTime - pausedDuration

                // Update meeting
                meetingRepository.finalizeMeeting(currentMeetingId, endTime, duration)
                meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.STOPPED)

                _recordingState.value = RecordingState.STOPPED

                // Release audio focus
                abandonAudioFocus()

                // Stop foreground and service
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                Log.d(TAG, "Recording stopped and finalized")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            audioFocusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            isAudioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            isAudioFocusGranted
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            isAudioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            isAudioFocusGranted
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
        isAudioFocusGranted = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_recordingState.value == RecordingState.RECORDING) {
                    pauseRecording(RecordingState.PAUSED_AUDIO_FOCUS_LOSS, "Audio focus lost")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_recordingState.value == RecordingState.PAUSED_AUDIO_FOCUS_LOSS) {
                    resumeRecording()
                }
            }
        }
    }

    private fun createNotification(message: String, showActions: Boolean = true): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recorder")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (showActions) {
            builder.addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )

            // Add pause/resume actions based on state
            if (_recordingState.value == RecordingState.PAUSED_AUDIO_FOCUS_LOSS) {
                val resumeIntent = Intent(this, RecordingService::class.java).apply {
                    action = ACTION_RESUME
                }
                val resumePendingIntent = PendingIntent.getService(
                    this,
                    1,
                    resumeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_play,
                    "Resume",
                    resumePendingIntent
                )
            }
        }

        return builder.build()
    }

    private fun showNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(message: String) {
        showNotification(message)
    }

    private fun cleanupResources() {
        chunkRecordingJob?.cancel()
        timerJob?.cancel()
        silenceDetectionJob?.cancel()

        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping media recorder", e)
            }
            release()
        }
        mediaRecorder = null
    }
}
