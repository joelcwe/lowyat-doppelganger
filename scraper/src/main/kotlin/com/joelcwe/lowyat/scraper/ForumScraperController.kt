package com.joelcwe.lowyat.scraper

import com.joelcwe.lowyat.scraper.model.Status
import com.joelcwe.lowyat.scraper.service.ForumScraper
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/scraper/api/v1/"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ForumScraperController(
    private val forumScraper: ForumScraper
) {

    @PostMapping(value = ["/status"])
    fun start(@RequestBody status: Status): String {
        var msg = ""
        if (status.action == "START") {
            if (!forumScraper.isScrapeJobActive()) {
                msg = "Starting scraping for topic: ${status.topic}."
                forumScraper.launchScrapeJob(status.topic)
            } else {
                msg = "Scraping already started."
            }
        } else if (status.action == "STOP") {
            msg = if (forumScraper.isScrapeJobActive()) {
                forumScraper.cancelScrapeJob()
                "Scraping cancel initiated."
            } else {
                "No running job found."
            }
        } else {
            msg = "Invalid action provided : ${status.action}! "
        }
        return msg
    }

    @GetMapping(value = ["/status"])
    fun status(): String {
        var msg = ""
        msg = if (forumScraper.isScrapeJobCompleted()) {
            "Scrape job has completed!"
        } else if (forumScraper.isScrapeJobActive()) {
            "Scraper Running!"
        } else "Scrapper has not started!"
        return msg

    }

}