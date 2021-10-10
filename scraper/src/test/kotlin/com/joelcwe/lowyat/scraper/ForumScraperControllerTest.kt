package com.joelcwe.lowyat.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.joelcwe.lowyat.scraper.model.Status
import com.joelcwe.lowyat.scraper.service.ForumScraper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@WebMvcTest
@TestMethodOrder(
    MethodOrderer.OrderAnnotation::class
)
internal class ForumScraperControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    private lateinit var forumScraper: ForumScraper

    //TODO Tests are ordered and are not independent which is not ideal..
    @Test
    @Order(1)
    fun `given no scrape job running when get start then return error`() {
        every { forumScraper.isScrapeJobCompleted() } returns false
        every { forumScraper.isScrapeJobActive() } returns false
        mockMvc.perform(get("/scraper/api/v1/status").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("Scrapper has not started!"))
    }

    @Test
    @Order(2)
    fun `given no scrape job running when post stop then return error`() {
        every { forumScraper.isScrapeJobActive() } returns false
        val status = Status("STOP", "test")
        mockMvc.perform(
            post("/scraper/api/v1/status").contentType(MediaType.APPLICATION_JSON).content(asJsonString(status))
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("No running job found."))
    }

    @Test
    @Order(3)
    fun `given no scrape job running when post invalid action then return error`() {
        val status = Status("BADCOMMAND", "test")
        mockMvc.perform(
            post("/scraper/api/v1/status").contentType(MediaType.APPLICATION_JSON).content(asJsonString(status))
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("Invalid action provided : ${status.action}! "))

    }


    @Test
    @Order(4)
    fun `given no scrape job running when post start then start the job`() {
        val status = Status("START", "test")
        every { forumScraper.isScrapeJobActive() } returns false
        coJustRun { forumScraper.launchScrapeJob("test") }
        mockMvc.perform(
            post("/scraper/api/v1/status").contentType(MediaType.APPLICATION_JSON).content(asJsonString(status))
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("Starting scraping for topic: ${status.topic}."))
        verify { forumScraper.launchScrapeJob("test") }
    }

    @Test
    @Order(5)
    fun `given running job when post stop then stop the job`() {
        val status = Status("STOP", "test")
        every { forumScraper.isScrapeJobActive() } returns true
        justRun { forumScraper.cancelScrapeJob() }
        mockMvc.perform(
            post("/scraper/api/v1/status").contentType(MediaType.APPLICATION_JSON).content(asJsonString(status))
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("Scraping cancel initiated."))
        verify { forumScraper.cancelScrapeJob() }
    }


    private fun asJsonString(obj: Any?): String {
        return try {
            ObjectMapper().writeValueAsString(obj)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }


}