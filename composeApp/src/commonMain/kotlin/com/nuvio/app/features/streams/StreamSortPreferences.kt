package com.nuvio.app.features.streams

import com.nuvio.app.features.debrid.DebridStreamEncode
import com.nuvio.app.features.debrid.DebridStreamLanguage
import com.nuvio.app.features.debrid.DebridStreamMetadata
import com.nuvio.app.features.debrid.DebridStreamPreferences
import com.nuvio.app.features.debrid.DebridStreamResolution
import kotlinx.serialization.Serializable

@Serializable
enum class StreamSortKey {
    LANGUAGE,
    RESOLUTION,
    ENCODE,
    ;

    companion object {
        val defaultPriority = listOf(LANGUAGE, RESOLUTION, ENCODE)
    }
}

/**
 * Preferred orders used to sort every stream, across addons, and to pick the auto-play stream.
 * Each list is in preference order; values left out rank after the listed ones.
 */
@Serializable
data class StreamSortPreferences(
    val languages: List<DebridStreamLanguage> = emptyList(),
    val resolutions: List<DebridStreamResolution> = emptyList(),
    val encodes: List<DebridStreamEncode> = emptyList(),
    val priority: List<StreamSortKey> = StreamSortKey.defaultPriority,
) {
    val isActive: Boolean
        get() = languages.isNotEmpty() || resolutions.isNotEmpty() || encodes.isNotEmpty()

    fun normalized(): StreamSortPreferences = copy(
        languages = languages.distinct(),
        resolutions = resolutions.distinct(),
        encodes = encodes.distinct(),
        priority = (priority + StreamSortKey.defaultPriority).distinct(),
    )
}

object StreamSortPresentation {
    /** Stable sort: streams that rank equally keep their addon order. */
    fun sort(streams: List<StreamItem>, preferences: StreamSortPreferences): List<StreamItem> {
        if (!preferences.isActive || streams.size < 2) return streams
        val rankingPreferences = DebridStreamPreferences(
            preferredLanguages = preferences.languages,
            preferredResolutions = preferences.resolutions,
            preferredEncodes = preferences.encodes,
        )
        val priority = preferences.normalized().priority
        return streams
            .map { stream -> stream to DebridStreamMetadata.facts(stream, rankingPreferences) }
            .sortedWith { (_, left), (_, right) ->
                priority.firstNotNullOfOrNull { key ->
                    when (key) {
                        StreamSortKey.LANGUAGE -> left.languageRank.compareTo(right.languageRank)
                        StreamSortKey.RESOLUTION -> left.resolutionRank.compareTo(right.resolutionRank)
                        StreamSortKey.ENCODE -> left.encodeRank.compareTo(right.encodeRank)
                    }.takeIf { it != 0 }
                } ?: 0
            }
            .map { (stream, _) -> stream }
    }
}
