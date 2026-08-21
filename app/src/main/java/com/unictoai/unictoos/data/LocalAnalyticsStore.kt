package com.unictoai.unictoos.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.unictoai.unictoos.domain.SessionSummary
import com.unictoai.unictoos.domain.StreamHealthSample

/** A local-only analytics row. It deliberately contains no destination URL or credential. */
data class LocalAnalyticsSession(
    val id: String,
    val mode: String,
    val platform: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val durationSeconds: Long,
    val averageBitrateKbps: Int,
    val maxBitrateKbps: Int,
    val fps: Int,
    val droppedFrames: Int,
    val reconnectCount: Int,
    val recordingPath: String?,
    val sceneTemplate: String?,
)

data class LocalAnalyticsComparison(
    val sessionCount: Int,
    val averageDurationSeconds: Long,
    val averageBitrateKbps: Int,
    val averageDroppedFrames: Int,
    val reconnectingSessions: Int,
) {
    companion object {
        fun from(records: List<LocalAnalyticsSession>): LocalAnalyticsComparison {
            if (records.isEmpty()) return LocalAnalyticsComparison(0, 0L, 0, 0, 0)
            return LocalAnalyticsComparison(
                sessionCount = records.size,
                averageDurationSeconds = records.map { it.durationSeconds }.average().toLong(),
                averageBitrateKbps = records.map { it.averageBitrateKbps }.average().toInt(),
                averageDroppedFrames = records.map { it.droppedFrames.coerceAtLeast(0) }.average().toInt(),
                reconnectingSessions = records.count { it.reconnectCount > 0 },
            )
        }
    }
}

/** SQLite-backed local analytics. Records are bounded and can be deleted with app data. */
class LocalAnalyticsStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SESSIONS (
                id TEXT PRIMARY KEY NOT NULL,
                mode TEXT NOT NULL,
                platform TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                finished_at INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                average_bitrate_kbps INTEGER NOT NULL,
                max_bitrate_kbps INTEGER NOT NULL,
                fps INTEGER NOT NULL,
                dropped_frames INTEGER NOT NULL,
                reconnect_count INTEGER NOT NULL,
                recording_path TEXT,
                scene_template TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_sessions_finished_at ON $TABLE_SESSIONS(finished_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) onCreate(db)
    }

    /** Persist one completed session and retain only the newest bounded records. */
    fun recordSession(
        summary: SessionSummary,
        platform: String = "local",
        sceneTemplate: String? = null,
        reconnectCount: Int = 0,
        recordingPath: String? = null,
        healthSamples: List<StreamHealthSample> = emptyList(),
    ) {
        val validSamples = healthSamples.filter { it.bitrateKbps >= 0 }
        val values = ContentValues().apply {
            put("id", summary.id)
            put("mode", summary.mode.name)
            put("platform", platform.take(MAX_LABEL_CHARS))
            put("started_at", (summary.finishedAtMillis - summary.elapsedSeconds * 1_000L).coerceAtLeast(0L))
            put("finished_at", summary.finishedAtMillis)
            put("duration_seconds", summary.elapsedSeconds.coerceAtLeast(0L))
            put("average_bitrate_kbps", validSamples.map { it.bitrateKbps }.ifEmpty { listOf(summary.bitrateKbps) }.average().toInt().coerceAtLeast(0))
            put("max_bitrate_kbps", validSamples.maxOfOrNull { it.bitrateKbps } ?: summary.bitrateKbps.coerceAtLeast(0))
            put("fps", summary.fps.coerceAtLeast(0))
            put("dropped_frames", summary.droppedFrames)
            put("reconnect_count", reconnectCount.coerceAtLeast(0))
            put("recording_path", recordingPath?.take(MAX_PATH_CHARS))
            put("scene_template", sceneTemplate?.take(MAX_LABEL_CHARS))
        }
        writableDatabase.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        writableDatabase.execSQL(
            "DELETE FROM $TABLE_SESSIONS WHERE id NOT IN (SELECT id FROM $TABLE_SESSIONS ORDER BY finished_at DESC LIMIT ?)",
            arrayOf(MAX_RECORDS.toString()),
        )
    }

    /** Read recent records in newest-first order for the local Library screen. */
    fun loadRecent(limit: Int = MAX_RECORDS): List<LocalAnalyticsSession> {
        val safeLimit = limit.coerceIn(1, MAX_RECORDS)
        return readableDatabase.query(
            TABLE_SESSIONS,
            COLUMNS,
            null,
            null,
            null,
            null,
            "finished_at DESC",
            safeLimit.toString(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    LocalAnalyticsSession(
                        id = cursor.getString(0),
                        mode = cursor.getString(1),
                        platform = cursor.getString(2),
                        startedAtMillis = cursor.getLong(3),
                        finishedAtMillis = cursor.getLong(4),
                        durationSeconds = cursor.getLong(5),
                        averageBitrateKbps = cursor.getInt(6),
                        maxBitrateKbps = cursor.getInt(7),
                        fps = cursor.getInt(8),
                        droppedFrames = cursor.getInt(9),
                        reconnectCount = cursor.getInt(10),
                        recordingPath = cursor.getStringOrNull(11),
                        sceneTemplate = cursor.getStringOrNull(12),
                    ),
                )
            }
        }
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

    private companion object {
        const val DATABASE_NAME = "unictoos_analytics.db"
        const val DATABASE_VERSION = 1
        const val TABLE_SESSIONS = "streaming_sessions"
        const val MAX_RECORDS = 120
        const val MAX_LABEL_CHARS = 80
        const val MAX_PATH_CHARS = 512
        val COLUMNS = arrayOf(
            "id", "mode", "platform", "started_at", "finished_at", "duration_seconds",
            "average_bitrate_kbps", "max_bitrate_kbps", "fps", "dropped_frames", "reconnect_count",
            "recording_path", "scene_template",
        )
    }
}
