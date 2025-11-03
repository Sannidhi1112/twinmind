package com.twinmind.voicerecorder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.data.local.entity.Meeting
import com.twinmind.voicerecorder.data.repository.MeetingRepository
import com.twinmind.voicerecorder.data.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meetingRepository: MeetingRepository,
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val meetingId: Long = savedStateHandle.get<String>("meetingId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    val meeting: StateFlow<Meeting?> = meetingRepository.getMeetingByIdFlow(meetingId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    companion object {
        private const val TAG = "SummaryViewModel"
    }

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            try {
                if (meetingId == -1L) {
                    _uiState.value = SummaryUiState.Error("Invalid meeting ID")
                    return@launch
                }

                // Check if summary already exists
                val existingSummary = summaryRepository.getSummaryData(meetingId)
                if (existingSummary != null && existingSummary.status == com.twinmind.voicerecorder.data.local.entity.SummaryStatus.COMPLETED) {
                    _uiState.value = SummaryUiState.Success(
                        title = existingSummary.title,
                        summary = existingSummary.summary,
                        actionItems = existingSummary.actionItems,
                        keyPoints = existingSummary.keyPoints
                    )
                    return@launch
                }

                // Generate new summary
                generateSummary()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading summary", e)
                _uiState.value = SummaryUiState.Error(e.message ?: "Failed to load summary")
            }
        }
    }

    fun generateSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = SummaryUiState.Loading

                summaryRepository.generateSummary(meetingId).collect { progress ->
                    when (progress) {
                        is SummaryRepository.SummaryProgress.Loading -> {
                            _uiState.value = SummaryUiState.Loading
                        }
                        is SummaryRepository.SummaryProgress.Success -> {
                            _uiState.value = SummaryUiState.Success(
                                title = progress.title,
                                summary = progress.summary,
                                actionItems = progress.actionItems,
                                keyPoints = progress.keyPoints
                            )
                        }
                        is SummaryRepository.SummaryProgress.Error -> {
                            _uiState.value = SummaryUiState.Error(progress.message)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating summary", e)
                _uiState.value = SummaryUiState.Error(e.message ?: "Failed to generate summary")
            }
        }
    }

    sealed class SummaryUiState {
        object Loading : SummaryUiState()
        data class Success(
            val title: String,
            val summary: String,
            val actionItems: List<String>,
            val keyPoints: List<String>
        ) : SummaryUiState()
        data class Error(val message: String) : SummaryUiState()
    }
}
