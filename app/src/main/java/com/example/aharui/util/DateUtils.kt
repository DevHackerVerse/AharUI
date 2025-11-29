package com.example.aharui.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

    fun getTodayString(): String {
        return dateFormat.format(Date())
    }

    fun getDateString(date: Date): String {
        return dateFormat.format(date)
    }

    fun parseDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun getDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(calendar.time)
    }

    fun getWeekAgo(): String = getDaysAgo(7)
    fun getMonthAgo(): String = getDaysAgo(30)
    fun getSixMonthsAgo(): String = getDaysAgo(180)

    fun getDayStartTimestamp(date: Date): Long {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getDayEndTimestamp(date: Date): Long {
        return getDayStartTimestamp(date) + 24 * 60 * 60 * 1000
    }

    /**
     * Generate list of all dates between startDate and endDate
     */
    fun getDatesBetween(startDate: String, endDate: String): List<String> {
        val dates = mutableListOf<String>()
        val start = parseDate(startDate) ?: return emptyList()
        val end = parseDate(endDate) ?: return emptyList()

        val calendar = Calendar.getInstance()
        calendar.time = start

        while (!calendar.time.after(end)) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dates
    }

    /**
     * Get formatted labels based on chart type
     */
    fun getDateRangeLabels(startDate: String, endDate: String, type: ChartType): List<String> {
        val dates = getDatesBetween(startDate, endDate)

        return when (type) {
            ChartType.WEEKLY -> {
                // Return day labels (Mon, Tue, etc.)
                dates.map { dateString ->
                    parseDate(dateString)?.let { date ->
                        SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                    } ?: ""
                }
            }
            ChartType.MONTHLY -> {
                // Return date labels (1, 5, 10, 15, etc.)
                dates.filterIndexed { index, _ -> index % 5 == 0 }
                    .map { dateString ->
                        parseDate(dateString)?.let { date ->
                            SimpleDateFormat("d", Locale.getDefault()).format(date)
                        } ?: ""
                    }
            }
            ChartType.SIX_MONTHS -> {
                // Return month labels
                val seenMonths = mutableSetOf<String>()
                dates.mapNotNull { dateString ->
                    parseDate(dateString)?.let { date ->
                        val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
                        if (seenMonths.add(monthLabel)) monthLabel else null
                    }
                }
            }
        }
    }

    enum class ChartType {
        WEEKLY, MONTHLY, SIX_MONTHS
    }
}