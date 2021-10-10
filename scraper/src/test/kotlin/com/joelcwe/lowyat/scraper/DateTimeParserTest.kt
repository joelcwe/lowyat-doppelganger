package com.joelcwe.lowyat.scraper

import com.joelcwe.lowyat.scraper.service.DateTimeParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class DateTimeParserTest {

    private val dateTimeParser = DateTimeParser()

    @Test
    fun `given invalid date string when parse then min date time object returned`() {
        val invalidDate = " Test Date, 11:28 AM "
        val test = dateTimeParser.parse(invalidDate)
        assertEquals(LocalDateTime.MIN, test)
    }

    @Test
    fun `given invalid time string when parse then min date time object returned`() {
        val invalidTime = " Today, invalid date "
        assertEquals(LocalDateTime.MIN, dateTimeParser.parse(invalidTime))
    }

    @Test
    fun `given invalid string when parse then min date time object returned`() {
        val invalidString = "No comma separated elements"
        assertEquals(LocalDateTime.MIN, dateTimeParser.parse(invalidString))

    }

    @Test
    fun `given relative yesterday when parse then correct date time object returned`() {
        val todayString = " Yesterday, 12:00 PM "
        assertEquals(
            LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(12, 0)),
            dateTimeParser.parse(todayString)
        )
    }

    @Test
    fun `given arbitrary date when parse then correct date time object returned`() {
        val randomDateString = "Dec 1 2018, 10:07 PM, updated 3y ago"
        assertEquals(
            LocalDateTime.of(2018, 12, 1, 22, 7),
            dateTimeParser.parse(randomDateString)
        )
    }

    @Test
    fun `given arbitrary date 2 when parse then correct date time object returned`() {
        val randomDateString = "Dec 11 2018, 10:07 PM, updated 3y ago"
        assertEquals(
            LocalDateTime.of(2018, 12, 11, 22, 7),
            dateTimeParser.parse(randomDateString)
        )
    }


}