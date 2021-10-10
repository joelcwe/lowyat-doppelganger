package com.joelcwe.lowyat.scraper.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DocumentRetriever {

    private val rootUrl = "https://forum.lowyat.net/"

    fun getDocument(topic: String): Document {
        var document = Document("")
        runBlocking {
            runCatching {
                val response = Jsoup.connect(rootUrl + topic).userAgent("jsoup_scraper").timeout(5000).execute()
                if (response.statusCode() == 200) {
                    document = response.parse()
                }
                delay(300)

            }.onFailure { error -> logger.error("Failed to load page, connection error", error) }
        }
        return document
    }

    fun getDocumentNumPages(document: Document): Int {
        val elements = document.select("table td a[href]:contains(Pages)")
        return elements.first()?.text()?.filter { it.isDigit() }?.toInt() ?: 1
    }
}