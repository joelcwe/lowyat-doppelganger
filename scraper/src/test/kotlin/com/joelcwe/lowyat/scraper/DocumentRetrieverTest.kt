package com.joelcwe.lowyat.scraper

import com.joelcwe.lowyat.scraper.service.DocumentRetriever
import io.mockk.junit5.MockKExtension
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DocumentRetrieverTest {

    private val documentRetriever = DocumentRetriever()
    private fun readResourceFile(fileName: String) = this::class.java.getResource(fileName)?.readText(Charsets.UTF_8)

    @Test
    fun `given valid topic page when getTopicNumPages then parse number of pages`() {
        val pages = readResourceFile("/testForumTopicList.html")?.let { Jsoup.parse(it) }
            ?.let { documentRetriever.getDocumentNumPages(it) }
        assertEquals(10, pages)
    }

    @Test
    fun `given no pages element when getTopicNumPages then zero pages returned`() {
        val missingPagesDocument = Jsoup.parse("<div>Test</div>")
        assertEquals(1, documentRetriever.getDocumentNumPages(missingPagesDocument))
    }
}