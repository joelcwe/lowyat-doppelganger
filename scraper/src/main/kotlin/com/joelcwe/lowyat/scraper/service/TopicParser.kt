package com.joelcwe.lowyat.scraper.service

import com.joelcwe.lowyat.scraper.model.Post
import com.joelcwe.lowyat.scraper.repository.PostRepository
import mu.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.data.elasticsearch.core.join.JoinField
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

@Service
class TopicParser(
    val documentRetriever: DocumentRetriever,
    val dateTimeParser: DateTimeParser,
    val postRepository: PostRepository
) {

    fun parsePostDocument(elements: List<Element>, topic: Post) {
        elements.forEach {
            val postId = getPostId(it)
            val posterUsername = getPosterUsername(it)
            val postDate = getPostDate(it)
            val postText = getPostText(it)
            val post = Post(
                id = postId,
                content = postText ?: "REMOVED",
                dateTime = postDate,
                type = JoinField("post", topic.id),
                username = posterUsername,
                topicId = topic.topicId
            )
            kotlin.runCatching {
                //TODO handle retries when persisting a post fails
                postRepository.save(post)
            }.onFailure { error ->
                logger.error("Exception while storing $post into elasticsearch. ", error)
            }

        }
    }

    fun parseTopicThread(topicId: Int) {
        var document = documentRetriever.getDocument("topic/$topicId")
        val numPages = documentRetriever.getDocumentNumPages(document)
        var postOffset = 0
        var savedTopic: Post? = null
        while ((postOffset + 20) / 20 <= numPages) {
            val nextPage = "topic/$topicId/+$postOffset"
            document = documentRetriever.getDocument(nextPage)
            val elements = document.select("table.post_table tbody")
            if (postOffset == 0) {
                val topicTitle = getTopicTitle(document)
                val topicString: Element? = elements.first()
                if (topicString !== null) {
                    val postId = getPostId(topicString)
                    val posterUsername = getPosterUsername(topicString)
                    val postDate = getPostDate(topicString)
                    val postText = getPostText(topicString)
                    val topic = Post(
                        id = postId,
                        title = topicTitle,
                        content = postText!!,
                        dateTime =postDate,
                        type = JoinField("topic"),
                        username = posterUsername,
                        topicId = topicId
                    )
                    kotlin.runCatching {
                        //TODO handle retries when persisting a post fails
                        savedTopic = postRepository.save(topic)
                    }.onFailure { error ->
                        logger.error("Exception while storing $topic into elasticsearch. ", error)
                        return
                    }.onSuccess {
                        parsePostDocument(
                            elements.drop(1),
                            topic
                        )
                    }


                } else {
                    logger.error("Error parsing topic post. ")
                    break
                }
            } else {
                if (savedTopic != null) {
                    //https://youtrack.jetbrains.com/issue/KT-19446
                    parsePostDocument(elements, savedTopic!!)
                }
            }
            //rudimentary politeness delay
            Thread.sleep(1_000)
            postOffset += 20
        }
    }

    private fun getPostId(element: Element): String = element.parent().id()

    private fun getPosterUsername(element: Element): String = element.select(".row2.post_td_left a").text()

    private fun getPostText(element: Element): String? =
        element.select("td[valign].post_td_right div.post_text").first()?.ownText()

    private fun getPostDate(element: Element): Instant = dateTimeParser.parse(
        element.select(".post_td_right div[style=float: left;] span.postdetails").text()
    ).atZone((ZoneId.of("Asia/Kuala_Lumpur"))).toInstant()

    private fun getTopicTitle(document: Document): String = document.select("div.maintitle p:not(.expand)").text()

}