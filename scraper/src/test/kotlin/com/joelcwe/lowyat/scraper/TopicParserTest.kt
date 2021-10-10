package com.joelcwe.lowyat.scraper

import com.joelcwe.lowyat.scraper.model.Post
import com.joelcwe.lowyat.scraper.repository.PostRepository
import com.joelcwe.lowyat.scraper.service.DateTimeParser
import com.joelcwe.lowyat.scraper.service.DocumentRetriever
import com.joelcwe.lowyat.scraper.service.TopicParser
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.elasticsearch.core.join.JoinField
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class TopicParserTest {

    private val topic = Post("123123", null, "content", Instant.now(), JoinField("post", "topic_id"), "username", 213)
    private val documentRetriever = spyk(DocumentRetriever())
    private val dateTimeParser = DateTimeParser()
    private val postRepository = mockk<PostRepository> {
        every { save(any()) } returns topic
    }
    private val topicParser = spyk(TopicParser(documentRetriever, dateTimeParser, postRepository))

    private fun readResourceFile(fileName: String) = this::class.java.getResource(fileName)?.readText(Charsets.UTF_8)


    @Test
    fun `given valid forum page when parsePostDocument then store each posts`() {
        val elements = Jsoup.parse(readResourceFile("/testForumTopic.html")!!).select("table.post_table tbody")
        topicParser.parsePostDocument(elements, topic)
        verify(exactly = 20) { postRepository.save(any()) }
        confirmVerified(postRepository)
    }

    @Test
    fun `given valid single page forum topic when parseTopicThread then parse all posts`() {
        val document = Jsoup.parse(readResourceFile("/singlePageTopic.html")!!)
        every { documentRetriever.getDocument(any()) } returns document
        every { documentRetriever.getDocumentNumPages(any()) } returns 1
        topicParser.parseTopicThread(topic.id.toInt())
        verify(exactly = 16) { postRepository.save(any()) }
    }

    @Test
    fun `given valid multi page topic when parseTopicThread then parse all posts`() {
        val document = Jsoup.parse(readResourceFile("/testForumTopic.html")!!)
        every { documentRetriever.getDocument(any()) } returns document
        topicParser.parseTopicThread(topic.id.toInt())
        //6 pages x 20 posts per page
        verify(exactly = 120) { postRepository.save(any()) }
    }
}