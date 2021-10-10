package com.joelcwe.lowyat.scraper.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Service
class DateTimeParser {

    fun parse(relativeDate : String): LocalDateTime {
        //split date time parsing since there can be a relative day.
        val elements = relativeDate.split(",")
        var dateTime = LocalDateTime.MIN
        kotlin.runCatching {
            val date = parseDate(elements.first().trim())
            val time = parseTime(elements[1].trim())
            dateTime = LocalDateTime.of(date, time)
        }.onFailure { error -> logger.error("Date time parse exception", error) }

        return dateTime
    }

    private fun parseDate(day: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("MMM d yyyy")
        val date = when (day) {
            "Today" -> LocalDate.now()
            "Yesterday" -> LocalDate.now().minusDays(1)
            else -> LocalDate.parse(day, formatter)
        }
        return date
    }

    private fun parseTime(time: String): LocalTime {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return LocalTime.parse(time, formatter)
    }

}