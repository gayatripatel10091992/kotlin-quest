package com.gayatripatel.kotlinquest.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Daily streak with freezes. Each 7-day milestone earns a freeze (up to [MAX_FREEZES]); a freeze covers one
 * missed day. All dates are the learner's local calendar days, so the streak doesn't break at midnight UTC.
 */
data class Streak(
    val current: Int = 0,
    val longest: Int = 0,
    val lastActiveDay: LocalDate? = null,
    val freezes: Int = 0,
) {
    fun recordActivity(today: LocalDate): Streak {
        val last = lastActiveDay
        if (last != null && !today.isAfter(last)) return this // already counted today (or the clock went back)

        val missed = if (last == null) 0L else ChronoUnit.DAYS.between(last, today) - 1
        var freezesLeft = freezes
        val next = when {
            last == null -> 1
            missed == 0L -> current + 1
            missed <= freezesLeft -> {
                freezesLeft -= missed.toInt()
                current + 1
            }
            else -> 1
        }
        if (next % FREEZE_EVERY == 0) freezesLeft = (freezesLeft + 1).coerceAtMost(MAX_FREEZES)
        return Streak(next, maxOf(longest, next), today, freezesLeft)
    }

    /** The number to show today: still alive if the learner practised yesterday or freezes cover the gap. */
    fun display(today: LocalDate): Int {
        val last = lastActiveDay ?: return 0
        val missed = ChronoUnit.DAYS.between(last, today) - 1
        return if (missed <= freezes) current else 0
    }

    fun practisedOn(today: LocalDate): Boolean = lastActiveDay == today

    /** True when the streak is alive but today's lesson hasn't happened yet. */
    fun atRisk(today: LocalDate): Boolean = display(today) > 0 && !practisedOn(today)

    companion object {
        const val FREEZE_EVERY = 7
        const val MAX_FREEZES = 2
    }
}
