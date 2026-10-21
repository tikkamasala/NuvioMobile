package com.nuvio.app.features.streams

import com.nuvio.app.features.debrid.DebridStreamEncode
import com.nuvio.app.features.debrid.DebridStreamLanguage
import com.nuvio.app.features.debrid.DebridStreamResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class StreamSortPresentationTest {

    private val englishHd = stream("AddonA", "Movie.2024.1080p.WEB-DL.x264-GRP")
    private val french4k = stream("AddonB", "Movie.2024.MULTi.TRUEFRENCH.2160p.HEVC-GRP")
    private val frenchHd = stream("AddonB", "Movie 2024 1080p x265 🇫🇷")
    private val unknown = stream("AddonC", "Movie 2024")

    @Test
    fun `no preferences keeps the original order`() {
        val streams = listOf(englishHd, french4k, frenchHd)
        assertSame(streams, StreamSortPresentation.sort(streams, StreamSortPreferences()))
    }

    @Test
    fun `preferred language comes first across addons, detected from scene tags and flags`() {
        val sorted = StreamSortPresentation.sort(
            listOf(englishHd, unknown, french4k, frenchHd),
            StreamSortPreferences(languages = listOf(DebridStreamLanguage.FR)),
        )
        assertEquals(listOf(french4k, frenchHd, englishHd, unknown), sorted)
    }

    @Test
    fun `priority decides between language and resolution`() {
        val preferences = StreamSortPreferences(
            languages = listOf(DebridStreamLanguage.FR),
            resolutions = listOf(DebridStreamResolution.P1080, DebridStreamResolution.P2160),
        )
        val streams = listOf(englishHd, french4k, frenchHd)

        assertEquals(
            listOf(frenchHd, french4k, englishHd),
            StreamSortPresentation.sort(streams, preferences),
        )
        assertEquals(
            listOf(frenchHd, englishHd, french4k),
            StreamSortPresentation.sort(
                streams,
                preferences.copy(priority = listOf(StreamSortKey.RESOLUTION, StreamSortKey.LANGUAGE)),
            ),
        )
    }

    @Test
    fun `codec preference ranks unlisted codecs last`() {
        val sorted = StreamSortPresentation.sort(
            listOf(englishHd, french4k),
            StreamSortPreferences(encodes = listOf(DebridStreamEncode.HEVC)),
        )
        assertEquals(listOf(french4k, englishHd), sorted)
    }

    @Test
    fun `auto-play picks the first stream in preference order`() {
        val selected = StreamAutoPlaySelector.selectAutoPlayStream(
            streams = listOf(englishHd, frenchHd),
            mode = StreamAutoPlayMode.FIRST_STREAM,
            regexPattern = "",
            source = StreamAutoPlaySource.ALL_SOURCES,
            installedAddonNames = setOf("AddonA", "AddonB"),
            selectedAddons = emptySet(),
            selectedPlugins = emptySet(),
            sortPreferences = StreamSortPreferences(languages = listOf(DebridStreamLanguage.FR)),
        )
        assertEquals(frenchHd, selected)
    }

    private fun stream(addonName: String, name: String): StreamItem = StreamItem(
        name = name,
        url = "https://example.com/${name.hashCode()}.mkv",
        addonName = addonName,
        addonId = "addon:$addonName",
    )
}
