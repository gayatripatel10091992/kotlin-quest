package com.gayatripatel.kotlinquest.core

import java.time.LocalDate

/**
 * Spaced repetition with Leitner boxes. A right answer moves a card up a box and pushes its next review
 * further out (1, 2, 4, 8, 16 days); a wrong answer drops it back to box 1 and makes it due again today.
 */
data class ReviewCard(val exerciseId: String, val box: Int, val due: LocalDate) {
    fun reviewed(correct: Boolean, today: LocalDate): ReviewCard =
        if (correct) {
            val next = (box + 1).coerceAtMost(Leitner.MAX_BOX)
            copy(box = next, due = today.plusDays(Leitner.intervalDays(next)))
        } else {
            copy(box = 1, due = today)
        }
}

object Leitner {
    const val MAX_BOX = 5
    const val SESSION_SIZE = 6

    fun intervalDays(box: Int): Long = 1L shl (box.coerceIn(1, MAX_BOX) - 1)

    /**
     * Adds a finished lesson to the deck. Misses start in box 1 (due tomorrow), first-try answers in box 2.
     * Cards already in the deck keep their schedule unless the learner just missed them.
     */
    fun afterLesson(
        deck: Map<String, ReviewCard>,
        exerciseIds: List<String>,
        missed: Set<String>,
        today: LocalDate,
    ): Map<String, ReviewCard> {
        val updated = deck.toMutableMap()
        for (id in exerciseIds) {
            val existing = updated[id]
            updated[id] = when {
                id in missed -> ReviewCard(id, 1, today.plusDays(intervalDays(1)))
                existing != null -> existing
                else -> ReviewCard(id, 2, today.plusDays(intervalDays(2)))
            }
        }
        return updated
    }

    /** Cards due today or earlier: lowest box first, then the most overdue. */
    fun due(deck: Map<String, ReviewCard>, today: LocalDate, limit: Int = SESSION_SIZE): List<ReviewCard> =
        deck.values
            .filter { !it.due.isAfter(today) }
            .sortedWith(compareBy<ReviewCard> { it.box }.thenBy { it.due }.thenBy { it.exerciseId })
            .take(limit)

    fun dueCount(deck: Map<String, ReviewCard>, today: LocalDate): Int = deck.values.count { !it.due.isAfter(today) }

    /** How many cards sit in each box, for the "memory strength" bars. */
    fun boxCounts(deck: Map<String, ReviewCard>): List<Int> = (1..MAX_BOX).map { b -> deck.values.count { it.box == b } }
}
