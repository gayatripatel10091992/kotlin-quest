package com.gayatripatel.kotlinquest

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gayatripatel.kotlinquest.core.Answer
import com.gayatripatel.kotlinquest.core.CourseContent
import com.gayatripatel.kotlinquest.core.Exercise
import com.gayatripatel.kotlinquest.core.Hearts
import com.gayatripatel.kotlinquest.core.Leitner
import com.gayatripatel.kotlinquest.core.LessonResult
import com.gayatripatel.kotlinquest.core.LessonSession
import com.gayatripatel.kotlinquest.core.LessonState
import com.gayatripatel.kotlinquest.core.Progress
import com.gayatripatel.kotlinquest.core.Streak
import com.gayatripatel.kotlinquest.ui.Tab
import com.gayatripatel.kotlinquest.ui.TabShell
import com.gayatripatel.kotlinquest.ui.lesson.LessonScreen
import com.gayatripatel.kotlinquest.ui.lesson.LessonUiState
import com.gayatripatel.kotlinquest.ui.lesson.Selection
import com.gayatripatel.kotlinquest.ui.lesson.minutesToHeart
import com.gayatripatel.kotlinquest.ui.path.PathScreen
import com.gayatripatel.kotlinquest.ui.path.pathUiState
import com.gayatripatel.kotlinquest.ui.profile.ProfileScreen
import com.gayatripatel.kotlinquest.ui.profile.profileUiState
import com.gayatripatel.kotlinquest.ui.review.ReviewScreen
import com.gayatripatel.kotlinquest.ui.review.reviewUiState
import com.gayatripatel.kotlinquest.ui.theme.QuestTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM-rendered screenshots of every screen, driven by real core state rather than hand-made fakes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = Application::class)
class ScreenshotTest {
    private val course = CourseContent.course
    private val today = LocalDate.of(2026, 11, 27)
    private val now = 1_764_230_400_000L

    /** A learner a few days in: unit 1 done, halfway through unit 2, a few review cards due. */
    private val progress: Progress = run {
        var deck = Leitner.afterLesson(emptyMap(), course.lesson("ns-1")!!.exercises.map { it.id }, setOf("ns-1-3", "ns-1-5"), today.minusDays(3))
        deck = Leitner.afterLesson(deck, course.lesson("ns-2")!!.exercises.map { it.id }, setOf("ns-2-4"), today.minusDays(2))
        deck = Leitner.afterLesson(deck, course.lesson("ds-1")!!.exercises.map { it.id }, emptySet(), today)
        val history = listOf(0, 12, 25, 30, 0, 15, 20, 10, 0, 0, 22, 35, 20, 15, 5, 0, 25, 30, 40, 20, 0, 0, 15, 20, 30, 25, 20, 15, 30, 20, 25, 15, 30, 20, 15)
        Progress(
            totalXp = 640,
            dailyGoalXp = 20,
            hearts = Hearts(4, now - 6 * 60_000),
            streak = Streak(current = 12, longest = 12, lastActiveDay = today, freezes = 1),
            lessons = mapOf("ns-1" to 60, "ns-2" to 75, "ds-1" to 100),
            dailyXp = history.mapIndexed { i, xp -> today.minusDays((history.size - 1 - i).toLong()) to xp }.toMap(),
            deck = deck,
        )
    }

    private fun shot(name: String, dark: Boolean = false, tab: Tab? = null, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/roborazzi/$name.png") {
            QuestTheme(darkTheme = dark) {
                if (tab != null) TabShell(tab, {}) { m -> Box(m) { content() } } else content()
            }
        }
    }

    /** A lesson positioned on [exerciseId], as if the ones before it were answered right. */
    private fun at(lessonId: String, exerciseId: String, hearts: Hearts = Hearts(4, now)): LessonState {
        val start = LessonSession.start(course.lesson(lessonId)!!, hearts, now)
        return start.copy(queue = start.queue.dropWhile { it != exerciseId })
    }

    private fun lesson(name: String, state: LessonUiState, dark: Boolean = false) = shot(name, dark) {
        LessonScreen(state, {}, {}, {})
    }

    @Test fun path() = shot("01_path", tab = Tab.LEARN) {
        PathScreen(pathUiState(course, progress, now, today), {}, {})
    }

    @Test fun fix_the_bug() = lesson(
        "02_spot_the_bug",
        LessonUiState(title = "Nullable types", session = at("ns-1", "ns-1-5"), selection = Selection(option = 0)),
    )

    @Test fun arrange_tokens() {
        val exercise = course.exercise("ns-1-4") as Exercise.ArrangeTokens
        val bank = exercise.bank()
        val picked = listOf("val", "len", "=", "name", "?.").map { bank.indexOf(it) }
        lesson("03_build_the_line", LessonUiState(title = "Nullable types", session = at("ns-1", "ns-1-4"), selection = Selection(tokens = picked)))
    }

    @Test fun wrong_answer() {
        val session = LessonSession.answer(at("co-2", "co-2-1", Hearts.FULL), Answer.Option(2), now)
        lesson("04_not_quite", LessonUiState(title = "Group & find", session = session, selection = Selection(option = 2)))
    }

    @Test fun fill_blank_dark() = lesson(
        "05_fill_the_gap_dark",
        LessonUiState(title = "Smart casts & let", session = at("ns-2", "ns-2-3"), selection = Selection(option = 0)),
        dark = true,
    )

    @Test fun combo() {
        val session = LessonSession.answer(at("cr-2", "cr-2-2").copy(combo = 3, bestCombo = 3), Answer.Option(0), now)
        lesson("06_combo", LessonUiState(title = "Flow", session = session, selection = Selection(option = 0)))
    }

    @Test fun complete() {
        val result = LessonResult(
            lessonId = "ns-2", xp = 15, accuracyPercent = 100, firstTryCorrect = 4, total = 4, missed = emptySet(),
            bestCombo = 4, durationMillis = 134_000, hearts = Hearts.FULL, isReview = false,
        )
        val session = at("ns-2", "ns-2-1").copy(queue = emptyList(), phase = LessonState.Phase.Complete(result))
        lesson(
            "07_lesson_complete",
            LessonUiState(
                title = "Smart casts & let",
                session = session,
                streakAfter = 12,
                unlocked = course.units.first().caseStudies,
            ),
        )
    }

    @Test fun out_of_hearts() {
        val hearts = Hearts(0, now - 6 * 60_000)
        val session = at("ds-2", "ds-2-3", hearts).copy(phase = LessonState.Phase.OutOfHearts)
        lesson("08_out_of_hearts", LessonUiState(title = "Sealed types & when", session = session, minutesToHeart = minutesToHeart(hearts, now)))
    }

    @Test fun review() = shot("09_review", tab = Tab.REVIEW) {
        ReviewScreen(reviewUiState(course, progress, today)) {}
    }

    @Test fun profile() = shot("10_profile", tab = Tab.PROFILE) {
        ProfileScreen(profileUiState(course, progress, today)) {}
    }

    @Test fun path_dark() = shot("11_path_dark", dark = true, tab = Tab.LEARN) {
        PathScreen(pathUiState(course, progress, now, today), {}, {})
    }
}
