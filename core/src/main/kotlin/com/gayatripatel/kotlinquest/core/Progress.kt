package com.gayatripatel.kotlinquest.core

import java.time.LocalDate

/** Everything the app persists about a learner. */
data class Progress(
    val totalXp: Int = 0,
    val dailyGoalXp: Int = DEFAULT_GOAL,
    val hearts: Hearts = Hearts.FULL,
    val streak: Streak = Streak(),
    /** lessonId → best first-try accuracy, in percent. Presence means completed. */
    val lessons: Map<String, Int> = emptyMap(),
    val dailyXp: Map<LocalDate, Int> = emptyMap(),
    val deck: Map<String, ReviewCard> = emptyMap(),
) {
    fun xpOn(day: LocalDate): Int = dailyXp[day] ?: 0

    companion object {
        const val DEFAULT_GOAL = 20
        val GOALS = listOf(10, 20, 30, 50)

        /** Days of daily XP to keep: enough for the five-week activity grid. */
        const val HISTORY_DAYS = 35L
    }
}

/** Folds a finished lesson or review into the learner's progress. */
fun Progress.withResult(result: LessonResult, lessonExerciseIds: List<String>, today: LocalDate): Progress {
    val cutoff = today.minusDays(Progress.HISTORY_DAYS)
    val days = (dailyXp + (today to xpOn(today) + result.xp)).filterKeys { it.isAfter(cutoff) }
    val bestLessons = if (result.isReview) lessons
    else lessons + (result.lessonId to maxOf(lessons[result.lessonId] ?: 0, result.accuracyPercent))
    val newDeck = if (result.isReview) deck
    else Leitner.afterLesson(deck, lessonExerciseIds, result.missed, today)
    return copy(
        totalXp = totalXp + result.xp,
        hearts = result.hearts,
        streak = streak.recordActivity(today),
        lessons = bestLessons,
        dailyXp = days,
        deck = newDeck,
    )
}

/** Applies one review answer to the deck. Reviews update cards as they go, so quitting early loses nothing. */
fun Progress.reviewed(exerciseId: String, correct: Boolean, today: LocalDate): Progress {
    val card = deck[exerciseId] ?: return this
    return copy(deck = deck + (exerciseId to card.reviewed(correct, today)))
}

enum class NodeStatus { COMPLETED, CURRENT, LOCKED }

data class PathNode(val lesson: Lesson, val unit: CourseUnit, val status: NodeStatus, val bestAccuracy: Int?)

data class UnitProgress(val unit: CourseUnit, val nodes: List<PathNode>) {
    val completed: Boolean get() = nodes.all { it.status == NodeStatus.COMPLETED }
    val completedCount: Int get() = nodes.count { it.status == NodeStatus.COMPLETED }
}

/** Lessons unlock in order: everything up to the first unfinished lesson is open; that one is "current". */
object CoursePath {
    fun build(course: Course, progress: Progress): List<UnitProgress> {
        var currentAssigned = false
        return course.units.map { unit ->
            val nodes = unit.lessons.map { lesson ->
                val best = progress.lessons[lesson.id]
                val status = when {
                    best != null -> NodeStatus.COMPLETED
                    !currentAssigned -> NodeStatus.CURRENT.also { currentAssigned = true }
                    else -> NodeStatus.LOCKED
                }
                PathNode(lesson, unit, status, best)
            }
            UnitProgress(unit, nodes)
        }
    }

    fun current(course: Course, progress: Progress): PathNode? =
        build(course, progress).flatMap { it.nodes }.firstOrNull { it.status == NodeStatus.CURRENT }

    fun unlockedCaseStudies(course: Course, progress: Progress): List<CaseStudy> =
        build(course, progress).filter { it.completed }.flatMap { it.unit.caseStudies }

    fun isPlayable(course: Course, progress: Progress, lessonId: String): Boolean =
        build(course, progress).flatMap { it.nodes }.any { it.lesson.id == lessonId && it.status != NodeStatus.LOCKED }
}
