package com.gayatripatel.kotlinquest.core

/**
 * Lives for lessons. A mistake costs one; they refill one at a time every [REFILL_MILLIS].
 *
 * Only a count and an anchor time are stored, so no timer or WorkManager job has to run in the background:
 * the current value is worked out from the clock whenever it's read.
 */
data class Hearts(val count: Int = MAX, val refillAnchorMillis: Long? = null) {

    /** Applies any refills earned since [refillAnchorMillis]. */
    fun refreshed(nowMillis: Long): Hearts {
        if (count >= MAX || refillAnchorMillis == null) return FULL
        val earned = ((nowMillis - refillAnchorMillis) / REFILL_MILLIS).toInt().coerceAtLeast(0)
        val newCount = (count + earned).coerceAtMost(MAX)
        return if (newCount == MAX) FULL else Hearts(newCount, refillAnchorMillis + earned * REFILL_MILLIS)
    }

    fun lose(nowMillis: Long): Hearts {
        val current = refreshed(nowMillis)
        if (current.count == 0) return current
        return Hearts(current.count - 1, current.refillAnchorMillis ?: nowMillis)
    }

    fun current(nowMillis: Long): Int = refreshed(nowMillis).count

    /** Milliseconds until the next heart, or null when full. */
    fun millisToNext(nowMillis: Long): Long? {
        val current = refreshed(nowMillis)
        val anchor = current.refillAnchorMillis ?: return null
        return REFILL_MILLIS - (nowMillis - anchor)
    }

    companion object {
        const val MAX = 5
        const val REFILL_MILLIS = 30 * 60 * 1000L
        val FULL = Hearts(MAX, null)
    }
}
