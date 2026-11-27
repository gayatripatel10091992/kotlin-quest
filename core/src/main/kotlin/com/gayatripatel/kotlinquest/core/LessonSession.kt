package com.gayatripatel.kotlinquest.core

/**
 * A lesson as an immutable state machine: answer → feedback → continue, until the queue is empty or the
 * hearts run out. Like Duolingo, a missed exercise goes to the back of the queue and must be answered
 * again before the lesson ends. Every transition returns a new state, so the UI is a pure function of it
 * and the rules are unit-testable without Android.
 */
data class LessonState(
    val lessonId: String,
    val exercises: Map<String, Exercise>,
    val totalCount: Int,
    val queue: List<String>,
    val phase: Phase,
    val hearts: Hearts,
    val usesHearts: Boolean,
    val missed: Set<String> = emptySet(),
    val mistakes: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val startedAtMillis: Long,
) {
    sealed interface Phase {
        data object Answering : Phase
        data class Feedback(val correct: Boolean, val exerciseId: String) : Phase
        data object OutOfHearts : Phase
        data class Complete(val result: LessonResult) : Phase
    }

    val current: Exercise? get() = queue.firstOrNull()?.let { exercises[it] }

    /** Share of exercises cleared, for the progress bar. A missed one counts once it's answered right. */
    val progress: Float get() = if (totalCount == 0) 1f else (totalCount - queue.size).toFloat() / totalCount
}

data class LessonResult(
    val lessonId: String,
    val xp: Int,
    val accuracyPercent: Int,
    val firstTryCorrect: Int,
    val total: Int,
    val missed: Set<String>,
    val bestCombo: Int,
    val durationMillis: Long,
    val hearts: Hearts,
    val isReview: Boolean,
) {
    val perfect: Boolean get() = missed.isEmpty()
}

object LessonSession {
    const val LESSON_XP = 10
    const val PERFECT_BONUS = 5
    const val REVIEW_XP = 5

    fun start(lesson: Lesson, hearts: Hearts, nowMillis: Long): LessonState =
        create(lesson.id, lesson.exercises, hearts.refreshed(nowMillis), usesHearts = true, nowMillis)

    /** Practice from the review deck. Mistakes still send cards back to box 1 but don't cost hearts. */
    fun startReview(exercises: List<Exercise>, hearts: Hearts, nowMillis: Long): LessonState =
        create(REVIEW_ID, exercises, hearts.refreshed(nowMillis), usesHearts = false, nowMillis)

    private fun create(id: String, exercises: List<Exercise>, hearts: Hearts, usesHearts: Boolean, nowMillis: Long): LessonState {
        val phase = if (usesHearts && hearts.count == 0) LessonState.Phase.OutOfHearts else LessonState.Phase.Answering
        return LessonState(
            lessonId = id,
            exercises = exercises.associateBy { it.id },
            totalCount = exercises.size,
            queue = exercises.map { it.id },
            phase = phase,
            hearts = hearts,
            usesHearts = usesHearts,
            startedAtMillis = nowMillis,
        )
    }

    fun answer(state: LessonState, answer: Answer, nowMillis: Long): LessonState {
        if (state.phase != LessonState.Phase.Answering) return state
        val exercise = state.current ?: return state
        val correct = exercise.isCorrect(answer)
        val feedback = LessonState.Phase.Feedback(correct, exercise.id)
        return if (correct) {
            val combo = state.combo + 1
            state.copy(phase = feedback, combo = combo, bestCombo = maxOf(state.bestCombo, combo))
        } else {
            state.copy(
                phase = feedback,
                combo = 0,
                mistakes = state.mistakes + 1,
                missed = state.missed + exercise.id,
                hearts = if (state.usesHearts) state.hearts.lose(nowMillis) else state.hearts,
            )
        }
    }

    /** Leaves the feedback sheet: next exercise, the end screen, or the out-of-hearts screen. */
    fun proceed(state: LessonState, nowMillis: Long): LessonState {
        val feedback = state.phase as? LessonState.Phase.Feedback ?: return state
        val head = state.queue.first()
        val queue = if (feedback.correct) state.queue.drop(1) else state.queue.drop(1) + head
        val next = state.copy(queue = queue)
        return when {
            state.usesHearts && next.hearts.count == 0 -> next.copy(phase = LessonState.Phase.OutOfHearts)
            queue.isEmpty() -> next.copy(phase = LessonState.Phase.Complete(result(next, nowMillis)))
            else -> next.copy(phase = LessonState.Phase.Answering)
        }
    }

    private fun result(state: LessonState, nowMillis: Long): LessonResult {
        val firstTry = state.totalCount - state.missed.size
        val isReview = state.lessonId == REVIEW_ID
        val xp = when {
            isReview -> REVIEW_XP
            state.missed.isEmpty() -> LESSON_XP + PERFECT_BONUS
            else -> LESSON_XP
        }
        return LessonResult(
            lessonId = state.lessonId,
            xp = xp,
            accuracyPercent = if (state.totalCount == 0) 100 else firstTry * 100 / state.totalCount,
            firstTryCorrect = firstTry,
            total = state.totalCount,
            missed = state.missed,
            bestCombo = state.bestCombo,
            durationMillis = nowMillis - state.startedAtMillis,
            hearts = state.hearts,
            isReview = isReview,
        )
    }

    const val REVIEW_ID = "review"
}
