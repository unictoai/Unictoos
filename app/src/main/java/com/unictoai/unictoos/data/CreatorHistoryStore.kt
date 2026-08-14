package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.SessionMode
import com.unictoai.unictoos.domain.SessionSummary
import com.unictoai.unictoos.domain.StreamMarker
import com.unictoai.unictoos.domain.StreamHealthSample
import org.json.JSONArray
import org.json.JSONObject

class CreatorHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences("unictoos_creator_history", Context.MODE_PRIVATE)

    fun loadSessions(): List<SessionSummary> = runCatching {
        JSONArray(preferences.getString(KEY_SESSIONS, "[]")).toList { json ->
            SessionSummary(
                id = json.optString("id"),
                mode = runCatching { SessionMode.valueOf(json.optString("mode")) }.getOrDefault(SessionMode.BROADCAST),
                elapsedSeconds = json.optLong("elapsedSeconds"),
                bitrateKbps = json.optInt("bitrateKbps"),
                fps = json.optInt("fps"),
                droppedFrames = json.optInt("droppedFrames"),
                finishedAtMillis = json.optLong("finishedAtMillis"),
            )
        }
    }.getOrDefault(emptyList())

    fun addSession(summary: SessionSummary) {
        val sessions = (loadSessions() + summary).takeLast(MAX_ITEMS)
        preferences.edit().putString(KEY_SESSIONS, sessions.toJson { session ->
            JSONObject().apply {
                put("id", session.id)
                put("mode", session.mode.name)
                put("elapsedSeconds", session.elapsedSeconds)
                put("bitrateKbps", session.bitrateKbps)
                put("fps", session.fps)
                put("droppedFrames", session.droppedFrames)
                put("finishedAtMillis", session.finishedAtMillis)
            }
        }.toString()).apply()
    }

    fun loadMarkers(): List<StreamMarker> = runCatching {
        JSONArray(preferences.getString(KEY_MARKERS, "[]")).toList { json ->
            StreamMarker(json.optString("id"), json.optString("label"), json.optLong("elapsedSeconds"), json.optLong("createdAtMillis"))
        }
    }.getOrDefault(emptyList())

    fun loadHealthSamples(): List<StreamHealthSample> = runCatching {
        JSONArray(preferences.getString(KEY_HEALTH, "[]")).toList { json ->
            StreamHealthSample(
                elapsedSeconds = json.optLong("elapsedSeconds"),
                sessionId = json.optString("sessionId"),
                bitrateKbps = json.optInt("bitrateKbps"),
                fps = json.optInt("fps"),
                droppedFrames = json.optInt("droppedFrames"),
                audioLevel = json.optInt("audioLevel"),
                batteryPercent = json.optInt("batteryPercent"),
                thermalStatus = json.optInt("thermalStatus"),
                networkLabel = json.optString("networkLabel"),
            )
        }
    }.getOrDefault(emptyList())

    fun addHealthSamples(samples: List<StreamHealthSample>) {
        if (samples.isEmpty()) return
        val stored = (loadHealthSamples() + samples).takeLast(MAX_HEALTH_ITEMS)
        preferences.edit().putString(KEY_HEALTH, stored.toJson { sample ->
            JSONObject().apply {
                put("elapsedSeconds", sample.elapsedSeconds)
                put("sessionId", sample.sessionId)
                put("bitrateKbps", sample.bitrateKbps)
                put("fps", sample.fps)
                put("droppedFrames", sample.droppedFrames)
                put("audioLevel", sample.audioLevel)
                put("batteryPercent", sample.batteryPercent)
                put("thermalStatus", sample.thermalStatus)
                put("networkLabel", sample.networkLabel)
            }
        }.toString()).apply()
    }

    fun addMarker(marker: StreamMarker) {
        val markers = (loadMarkers() + marker).takeLast(MAX_ITEMS)
        preferences.edit().putString(KEY_MARKERS, markers.toJson { item ->
            JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("elapsedSeconds", item.elapsedSeconds)
                put("createdAtMillis", item.createdAtMillis)
            }
        }.toString()).apply()
    }

    private fun <T> List<T>.toJson(mapper: (T) -> JSONObject): JSONArray = JSONArray().also { array -> forEach { array.put(mapper(it)) } }

    private fun <T> JSONArray.toList(mapper: (JSONObject) -> T): List<T> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { add(mapper(it)) }
    }

    private companion object {
        const val MAX_ITEMS = 120
        const val KEY_SESSIONS = "sessions_v1"
        const val KEY_MARKERS = "markers_v1"
        const val KEY_HEALTH = "health_samples_v1"
        const val MAX_HEALTH_ITEMS = 1_200
    }
}
