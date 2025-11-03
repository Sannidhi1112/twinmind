package com.twinmind.voicerecorder.utils

import android.content.Context
import android.os.StatFs
import android.os.Environment
import java.io.File

object StorageUtils {

    private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100 MB minimum

    fun hasEnoughStorage(context: Context): Boolean {
        return getAvailableStorageBytes() > MIN_STORAGE_BYTES
    }

    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun getAvailableStorageMB(): Long {
        return getAvailableStorageBytes() / (1024 * 1024)
    }

    fun createRecordingsDirectory(context: Context): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAudioChunkFile(context: Context, meetingId: Long, chunkNumber: Int): File {
        val recordingsDir = createRecordingsDirectory(context)
        val meetingDir = File(recordingsDir, "meeting_$meetingId")
        if (!meetingDir.exists()) {
            meetingDir.mkdirs()
        }
        return File(meetingDir, "chunk_${chunkNumber}.m4a")
    }

    fun deleteMeetingDirectory(context: Context, meetingId: Long): Boolean {
        val recordingsDir = createRecordingsDirectory(context)
        val meetingDir = File(recordingsDir, "meeting_$meetingId")
        return if (meetingDir.exists()) {
            meetingDir.deleteRecursively()
        } else {
            false
        }
    }

    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0
    }
}
