package com.gayatripatel.kotlinquest.core

import java.time.LocalDate

/**
 * A small line-based format for [Progress], stored as one DataStore string:
 *
 * ```
 * v=1
 * xp=135
 * goal=20
 * hearts=4,1764230400000
 * streak=3,9,2026-11-26,1
 * lesson=ns-1:80
 * day=2026-11-26:25
 * card=ns-1-2:1:2026-11-27
 * ```
 *
 * It fails soft: a malformed line is skipped rather than throwing, so one bad value can't lock someone out of
 * their progress. Unknown keys are ignored, so older app versions can read files written by newer ones.
 */
object ProgressCodec {
    private const val VERSION = 1

    fun encode(p: Progress): String = buildString {
        appendLine("v=$VERSION")
        appendLine("xp=${p.totalXp}")
        appendLine("goal=${p.dailyGoalXp}")
        appendLine("hearts=${p.hearts.count},${p.hearts.refillAnchorMillis ?: ""}")
        appendLine("streak=${p.streak.current},${p.streak.longest},${p.streak.lastActiveDay ?: ""},${p.streak.freezes}")
        p.lessons.toSortedMap().forEach { (id, best) -> appendLine("lesson=$id:$best") }
        p.dailyXp.toSortedMap().forEach { (day, xp) -> appendLine("day=$day:$xp") }
        p.deck.toSortedMap().forEach { (id, c) -> appendLine("card=$id:${c.box}:${c.due}") }
    }

    fun decode(text: String?): Progress {
        if (text.isNullOrBlank()) return Progress()
        var p = Progress()
        val lessons = mutableMapOf<String, Int>()
        val days = mutableMapOf<LocalDate, Int>()
        val deck = mutableMapOf<String, ReviewCard>()

        for (line in text.lineSequence()) {
            val key = line.substringBefore('=', "")
            val value = line.substringAfter('=', "")
            if (key.isEmpty()) continue
            runCatching {
                when (key) {
                    "xp" -> p = p.copy(totalXp = value.toInt().coerceAtLeast(0))
                    "goal" -> p = p.copy(dailyGoalXp = value.toInt().coerceIn(1, 500))
                    "hearts" -> {
                        val (count, anchor) = value.split(',', limit = 2)
                        p = p.copy(hearts = Hearts(count.toInt().coerceIn(0, Hearts.MAX), anchor.toLongOrNull()))
                    }
                    "streak" -> {
                        val (current, longest, last, freezes) = value.split(',')
                        p = p.copy(
                            streak = Streak(
                                current = current.toInt(),
                                longest = longest.toInt(),
                                lastActiveDay = last.takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                                freezes = freezes.toInt(),
                            ),
                        )
                    }
                    "lesson" -> lessons[value.substringBeforeLast(':')] = value.substringAfterLast(':').toInt()
                    "day" -> days[LocalDate.parse(value.substringBefore(':'))] = value.substringAfter(':').toInt()
                    "card" -> {
                        val parts = value.split(':')
                        val card = ReviewCard(parts[0], parts[1].toInt().coerceIn(1, Leitner.MAX_BOX), LocalDate.parse(parts[2]))
                        deck[card.exerciseId] = card
                    }
                }
                Unit
            }
        }
        return p.copy(lessons = lessons, dailyXp = days, deck = deck)
    }
}
