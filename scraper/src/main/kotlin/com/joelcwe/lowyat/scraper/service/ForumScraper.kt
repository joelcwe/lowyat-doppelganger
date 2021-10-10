package com.joelcwe.lowyat.scraper.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
private const val rootUrl = "https://forum.lowyat.net/"

@Service
class ForumScraper(
    val topicParser: TopicParser,
    val documentRetriever: DocumentRetriever,
    val crscope: CoroutineScope
) {

    //https://github.com/mockk/mockk/issues/284
    lateinit var scrapeJob: Job

    suspend fun parseTopicThreads(topic: String) {
        var doc = documentRetriever.getDocument(topic)
        val numPages = documentRetriever.getDocumentNumPages(doc)
        var postOffset = 0
        val topicIds = mutableSetOf<Int>()
        while ((postOffset + 30) / 30 <= numPages) {
            postOffset += 30
            val nextPage = "$topic/+$postOffset?prune_day=90&sort_key=last_post"
            doc = documentRetriever.getDocument(nextPage)
            getTopicUrlsFlow(doc).collect {
                if (!topicIds.contains(it)) {
                    topicParser.parseTopicThread(it)
                    topicIds.add(it)
                }
            }
        }
    }

    fun isScrapeJobActive() = if (this::scrapeJob.isInitialized) {
        scrapeJob.isActive || scrapeJob.isCancelled
    } else {
        false
    }

    fun isScrapeJobCompleted() = if (this::scrapeJob.isInitialized) {
        scrapeJob.isCompleted
    } else {
        false
    }


    fun launchScrapeJob(topic: String) {
        scrapeJob = crscope.launch {
            parseTopicThreads(topic)
        }
    }

    fun cancelScrapeJob() {
        if (this::scrapeJob.isInitialized) scrapeJob.cancel()
    }

    suspend fun getTopicUrlsFlow(document: Document) = channelFlow<Int> {
        val elements = document.select("td#forum_topic_title a")
        val topicIds = mutableSetOf<Int>()
        elements.filter { it.attr("href").startsWith("/topic") }
            .forEach {
                val temp = it.attr("href").split("/")
                runCatching {
                    println(temp[2].toInt())
                    send(temp[2].toInt())
                }.onFailure { error -> logger.error("Error storing topic ids", error) }
            }
    }


}