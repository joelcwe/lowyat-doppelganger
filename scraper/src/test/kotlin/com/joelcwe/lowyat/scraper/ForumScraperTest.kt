package com.joelcwe.lowyat.scraper

import app.cash.turbine.test
import com.joelcwe.lowyat.scraper.repository.PostRepository
import com.joelcwe.lowyat.scraper.service.DateTimeParser
import com.joelcwe.lowyat.scraper.service.DocumentRetriever
import com.joelcwe.lowyat.scraper.service.ForumScraper
import com.joelcwe.lowyat.scraper.service.TopicParser
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runBlockingTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(MockKExtension::class)
internal class ForumScraperTest {

    @JvmField
    @RegisterExtension
    val coroutinesTestExtension = CoroutineTestExtension()
    private var dateTimeParser = DateTimeParser()
    private val postRepository = mockk<PostRepository>()

    private var documentRetriever = spyk(DocumentRetriever())
    private var topicParser = spyk(TopicParser(documentRetriever, dateTimeParser, postRepository))
    private var forumScraper = spyk(ForumScraper(topicParser, documentRetriever, coroutinesTestExtension))

    private fun readResourceFile(fileName: String) = this::class.java.getResource(fileName)?.readText(Charsets.UTF_8)


    @Test
    fun `given valid topic list when getTopicUrlsFlow then parse topic links`() =
        coroutinesTestExtension.runBlockingTest {
            val flow: Flow<Int> = readResourceFile("/testForumTopicList.html")!!.let { Jsoup.parse(it) }
                .let { forumScraper.getTopicUrlsFlow(it) }

            flow.test {
                assertEquals(4934934, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given job started when isScrapeJobActive called then return true`() {
        forumScraper.scrapeJob = mockk {
            every { isActive } returns true
        }
        val test = forumScraper.isScrapeJobActive()
        println(test)
        assertEquals(true, test)
    }

    @Test
    fun `given valid topic list when parseTopicThreads collect all topic ids`() {
        every { documentRetriever.getDocument(any()) } returns
                readResourceFile("/testForumTopicList.html")?.let { Jsoup.parse(it) }!!
        justRun { topicParser.parseTopicThread(any()) }
        coroutinesTestExtension.runBlockingTest {
            forumScraper.parseTopicThreads("test")
            verify(exactly = 11) { documentRetriever.getDocument(any()) }
        }

    }
}