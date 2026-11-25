package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val weekday: String = ""
)

object JalaliCalendar {
    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    // Calendar.DAY_OF_WEEK: SUNDAY=1 .. SATURDAY=7
    private val WEEKDAYS_BY_CALENDAR_DAY = mapOf(
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنج‌شنبه",
        Calendar.FRIDAY to "جمعه",
        Calendar.SATURDAY to "شنبه"
    )

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    private val commaFormatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + if (days < 186) (days % 31) else ((days - 186) % 30)
        return JalaliDate(year = jy, month = jm, day = jd)
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) {
            n = n - jump + ((jump + 4) / 33) * 33
        }
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun getJalaliMonthDays(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isJalaliLeapYear(jy)) 30 else 29
    }

    fun addJalaliMonths(startYear: Int, startMonth: Int, startDay: Int, monthsToAdd: Int): JalaliDate {
        val totalMonths = (startYear * 12 + (startMonth - 1)) + monthsToAdd
        val year = Math.floorDiv(totalMonths, 12)
        val month = Math.floorMod(totalMonths, 12) + 1
        val maxDays = getJalaliMonthDays(year, month)
        val day = minOf(startDay, maxDays)
        return JalaliDate(year = year, month = month, day = day)
    }

    fun getTodayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        val j = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val weekday = WEEKDAYS_BY_CALENDAR_DAY[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        return j.copy(weekday = weekday)
    }

    fun monthName(month: Int): String = MONTHS.getOrElse(month - 1) { MONTHS.first() }

    fun toPersianDigits(value: Any): String {
        val s = value.toString()
        val sb = StringBuilder(s.length)
        for (c in s) {
            if (c in '0'..'9') sb.append(PERSIAN_DIGITS[c - '0']) else sb.append(c)
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            when (c) {
                in '۰'..'۹' -> sb.append(('0'.code + (c.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (c.code - '٠'.code)).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun formatMoney(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val formatted = commaFormatter.format(abs(amount))
        return sign + toPersianDigits(formatted)
    }

    fun formatInputWithCommas(raw: String): String {
        val digits = toEnglishDigits(raw).filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val parsed = digits.toLongOrNull() ?: return digits
        return commaFormatter.format(parsed)
    }

    fun parseTomanAmount(raw: String): Long? {
        val digits = toEnglishDigits(raw).filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits.toLongOrNull()
    }

    fun parseSignedTomanAmount(raw: String): Long? {
        val trimmed = toEnglishDigits(raw).trim()
        val negative = trimmed.startsWith("-")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return 0L
        val value = digits.toLongOrNull() ?: return null
        return if (negative) -value else value
    }

    fun formatMillions(amount: Long): String {
        if (amount == 0L) return "۰"
        val millions = amount / 1_000_000.0
        val rounded = kotlin.math.round(millions * 10.0) / 10.0
        val text = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(Locale.US, "%.1f", rounded)
        return toPersianDigits(text)
    }

    fun formatTodayHeader(today: JalaliDate = getTodayJalali()): String =
        "${today.weekday}، ${toPersianDigits(today.day)} ${monthName(today.month)} ${toPersianDigits(today.year)}"
}
