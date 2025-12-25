package com.ycngmn

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class AnimesamaProvider : MainAPI() {

    override var mainUrl = "https://anime-sama.si"
    override var name = "Anime-sama"
    override val supportedTypes = setOf(TvType.Anime)
    override var lang = "fr"

    // On désactive ce qui est cassé
    override val hasMainPage = false
    override val hasQuickSearch = false

    // -------- UTILS --------

    private fun Element.toSearchResult(): SearchResponse? {
        val link = this.attr("href")
        val title = this.text()
        if (link.isBlank() || title.isBlank()) return null

        return newAnimeSearchResponse(
            title,
            link,
            TvType.Anime
        )
    }

    // -------- LOAD ANIME --------

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1, h2, h3")?.text()
            ?: throw ErrorLoadingException("Titre introuvable")

        val poster = doc.selectFirst("img")?.attr("src")

        val episodes = mutableListOf<Episode>()

        // Anime-sama.si → liens vers pages épisodes
        doc.select("a[href*=\"episode\"]").forEachIndexed { index, el ->
            val epUrl = el.attr("href")
            episodes.add(
                newEpisode(epUrl) {
                    name = "Épisode ${index + 1}"
                    episode = index + 1
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            posterUrl = poster
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    // -------- LOAD LINKS --------

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document

        // Anime-sama.si → iframes
        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.startsWith("http")) {
                loadExtractor(
                    src,
                    data,
                    subtitleCallback,
                    callback
                )
            }
        }

        return true
    }
}