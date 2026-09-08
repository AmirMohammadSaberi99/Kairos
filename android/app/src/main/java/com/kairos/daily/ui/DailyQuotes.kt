package com.kairos.daily.ui

import java.time.LocalDate

data class DailyQuote(val text: String, val source: String)

object DailyQuotes {
    private val quotes = listOf(
        DailyQuote("If I have seen further, it is by standing on the shoulders of giants.", "Isaac Newton · letter to Robert Hooke, 1675"),
        DailyQuote("Chance favors only the prepared mind.", "Louis Pasteur · University of Lille lecture, 1854"),
        DailyQuote("Nothing is too wonderful to be true, if it be consistent with the laws of nature.", "Michael Faraday · laboratory diary, 1849"),
        DailyQuote("A person who dares to waste one hour of time has not discovered the value of life.", "Charles Darwin · Life and Letters, 1887"),
        DailyQuote("The first principle is that you must not fool yourself—and you are the easiest person to fool.", "Richard Feynman · Caltech address, 1974")
    )

    fun forDate(date: LocalDate = LocalDate.now()): DailyQuote = quotes[Math.floorMod(date.toEpochDay(), quotes.size.toLong()).toInt()]
}
