package com.gayatripatel.kotlinquest.core

import com.gayatripatel.kotlinquest.core.KotlinHighlighter.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExerciseTest {
    private val arrange = Exercise.ArrangeTokens(
        "t", "x", solution = listOf("val", "len", "=", "name", "?.", "length", "?:", "0"),
        distractors = listOf("!!"), explanation = "",
    )

    @Test fun arrange_grades_exact_order_only() {
        assertTrue(arrange.isCorrect(Answer.Tokens(arrange.solution)))
        assertFalse(arrange.isCorrect(Answer.Tokens(arrange.solution.reversed())))
        assertFalse(arrange.isCorrect(Answer.Option(0)))
    }

    @Test fun bank_is_stable_and_complete() {
        assertEquals(arrange.bank(), arrange.bank())
        assertEquals((arrange.solution + arrange.distractors).sorted(), arrange.bank().sorted())
    }

    @Test fun tokens_join_like_typed_code() {
        assertEquals("val len = name?.length ?: 0", CodeFormat.joinTokens(arrange.solution))
        assertEquals(
            "data class Song(val title: String)",
            CodeFormat.joinTokens(listOf("data", "class", "Song", "(", "val", "title", ":", "String", ")")),
        )
        assertEquals("val state = _state.asStateFlow()", CodeFormat.joinTokens(listOf("val", "state", "=", "_state", ".", "asStateFlow", "()")))
    }

    @Test fun fill_blank_shows_choice_in_place() {
        val e = Exercise.FillBlank("f", "", "val t = a ___ \"x\"", listOf("?:", "?."), 0, "")
        assertEquals("val t = a ?: \"x\"", e.filled(0))
        assertEquals("val t = a ___ \"x\"", e.filled(null))
    }

    @Test fun course_content_is_valid() {
        val course = CourseContent.course
        val ids = course.lessons.flatMap { l -> l.exercises.map { it.id } }
        assertEquals("exercise ids must be unique", ids.size, ids.toSet().size)
        assertTrue(course.units.all { it.caseStudies.isNotEmpty() && it.lessons.isNotEmpty() })
        course.lessons.flatMap { it.exercises }.filterIsInstance<Exercise.ArrangeTokens>().forEach {
            assertTrue("${it.id}: distractors must not be in the solution", it.distractors.none(it.solution::contains))
        }
    }
}

class HeartsTest {
    private val t0 = 1_000_000L

    @Test fun losing_starts_the_refill_clock() {
        val h = Hearts.FULL.lose(t0)
        assertEquals(4, h.count)
        assertEquals(t0, h.refillAnchorMillis)
        assertEquals(Hearts.REFILL_MILLIS, h.millisToNext(t0))
    }

    @Test fun refills_one_per_interval_and_keeps_remainder() {
        val h = Hearts.FULL.lose(t0).lose(t0).lose(t0) // 2 left
        val later = t0 + Hearts.REFILL_MILLIS + 5 * 60_000
        assertEquals(3, h.current(later))
        assertEquals(Hearts.REFILL_MILLIS - 5 * 60_000, h.millisToNext(later))
    }

    @Test fun caps_at_max_and_clears_anchor() {
        val h = Hearts.FULL.lose(t0)
        val full = h.refreshed(t0 + 10 * Hearts.REFILL_MILLIS)
        assertEquals(Hearts.FULL, full)
        assertNull(full.millisToNext(t0))
    }

    @Test fun never_goes_negative() {
        var h = Hearts.FULL
        repeat(10) { h = h.lose(t0) }
        assertEquals(0, h.count)
    }
}

class StreakTest {
    private val d = LocalDate.of(2026, 11, 20)

    @Test fun consecutive_days_extend() {
        val s = Streak().recordActivity(d).recordActivity(d.plusDays(1)).recordActivity(d.plusDays(1))
        assertEquals(2, s.current)
        assertEquals(2, s.longest)
    }

    @Test fun gap_resets_without_freezes() {
        val s = Streak(5, 5, d, 0).recordActivity(d.plusDays(3))
        assertEquals(1, s.current)
        assertEquals(5, s.longest)
        assertEquals(0, Streak(5, 5, d, 0).display(d.plusDays(2)))
    }

    @Test fun freeze_covers_a_missed_day() {
        val s = Streak(4, 4, d, 1).recordActivity(d.plusDays(2))
        assertEquals(5, s.current)
        assertEquals(0, s.freezes)
    }

    @Test fun seventh_day_earns_a_freeze() {
        val s = Streak(6, 6, d, 0).recordActivity(d.plusDays(1))
        assertEquals(7, s.current)
        assertEquals(1, s.freezes)
    }

    @Test fun at_risk_until_practised_today() {
        val s = Streak(3, 3, d, 0)
        assertTrue(s.atRisk(d.plusDays(1)))
        assertFalse(s.recordActivity(d.plusDays(1)).atRisk(d.plusDays(1)))
    }
}

class LeitnerTest {
    private val today = LocalDate.of(2026, 11, 27)

    @Test fun intervals_double() {
        assertEquals(listOf(1L, 2L, 4L, 8L, 16L), (1..5).map(Leitner::intervalDays))
    }

    @Test fun right_moves_up_wrong_drops_to_box_one() {
        val card = ReviewCard("a", 2, today)
        assertEquals(ReviewCard("a", 3, today.plusDays(4)), card.reviewed(true, today))
        assertEquals(ReviewCard("a", 1, today), card.reviewed(false, today))
        assertEquals(5, ReviewCard("a", 5, today).reviewed(true, today).box)
    }

    @Test fun lesson_seeds_deck_by_first_try_result() {
        val deck = Leitner.afterLesson(emptyMap(), listOf("a", "b"), missed = setOf("b"), today = today)
        assertEquals(ReviewCard("a", 2, today.plusDays(2)), deck["a"])
        assertEquals(ReviewCard("b", 1, today.plusDays(1)), deck["b"])
    }

    @Test fun due_orders_weakest_first() {
        val deck = mapOf(
            "x" to ReviewCard("x", 3, today.minusDays(1)),
            "y" to ReviewCard("y", 1, today),
            "z" to ReviewCard("z", 1, today.plusDays(1)),
        )
        assertEquals(listOf("y", "x"), Leitner.due(deck, today).map { it.exerciseId })
        assertEquals(2, Leitner.dueCount(deck, today))
    }
}

class LessonSessionTest {
    private val lesson = CourseContent.course.lessons.first()
    private val t0 = 5_000L

    private fun right(e: Exercise): Answer = when (e) {
        is Exercise.Choice -> Answer.Option(e.correctIndex)
        is Exercise.FillBlank -> Answer.Option(e.correctIndex)
        is Exercise.ArrangeTokens -> Answer.Tokens(e.solution)
    }

    private fun wrong(e: Exercise): Answer = when (e) {
        is Exercise.Choice -> Answer.Option((e.correctIndex + 1) % e.options.size)
        is Exercise.FillBlank -> Answer.Option((e.correctIndex + 1) % e.options.size)
        is Exercise.ArrangeTokens -> Answer.Tokens(e.solution.reversed())
    }

    @Test fun perfect_run_earns_bonus() {
        var s = LessonSession.start(lesson, Hearts.FULL, t0)
        while (s.phase == LessonState.Phase.Answering) {
            s = LessonSession.proceed(LessonSession.answer(s, right(s.current!!), t0), t0 + 60_000)
        }
        val result = (s.phase as LessonState.Phase.Complete).result
        assertEquals(LessonSession.LESSON_XP + LessonSession.PERFECT_BONUS, result.xp)
        assertEquals(100, result.accuracyPercent)
        assertEquals(lesson.exercises.size, result.bestCombo)
        assertEquals(60_000L, result.durationMillis)
    }

    @Test fun miss_costs_a_heart_and_requeues() {
        var s = LessonSession.start(lesson, Hearts.FULL, t0)
        val first = s.current!!
        s = LessonSession.answer(s, wrong(first), t0)
        assertEquals(LessonState.Phase.Feedback(false, first.id), s.phase)
        assertEquals(4, s.hearts.count)
        s = LessonSession.proceed(s, t0)
        assertEquals(first.id, s.queue.last())
        assertEquals(0f, s.progress)

        while (s.phase == LessonState.Phase.Answering) {
            s = LessonSession.proceed(LessonSession.answer(s, right(s.current!!), t0), t0)
        }
        val result = (s.phase as LessonState.Phase.Complete).result
        assertEquals(LessonSession.LESSON_XP, result.xp)
        assertEquals(setOf(first.id), result.missed)
        assertEquals((lesson.exercises.size - 1) * 100 / lesson.exercises.size, result.accuracyPercent)
    }

    @Test fun running_out_of_hearts_ends_lesson() {
        var s = LessonSession.start(lesson, Hearts(1, t0), t0)
        s = LessonSession.proceed(LessonSession.answer(s, wrong(s.current!!), t0), t0)
        assertEquals(LessonState.Phase.OutOfHearts, s.phase)
    }

    @Test fun review_mistakes_are_free() {
        val exercises = lesson.exercises.take(2)
        var s = LessonSession.startReview(exercises, Hearts(1, t0), t0)
        s = LessonSession.proceed(LessonSession.answer(s, wrong(s.current!!), t0), t0)
        assertEquals(LessonState.Phase.Answering, s.phase)
        assertEquals(1, s.hearts.count)
    }

    @Test fun answers_are_ignored_outside_answering() {
        val s = LessonSession.answer(LessonSession.start(lesson, Hearts.FULL, t0), Answer.Option(0), t0)
        assertEquals(s, LessonSession.answer(s, Answer.Option(1), t0))
    }
}

class ProgressTest {
    private val course = CourseContent.course
    private val today = LocalDate.of(2026, 11, 27)

    private fun result(lessonId: String, missed: Set<String> = emptySet()) = LessonResult(
        lessonId, xp = 15, accuracyPercent = 100, firstTryCorrect = 4, total = 4, missed = missed,
        bestCombo = 4, durationMillis = 1000, hearts = Hearts.FULL, isReview = false,
    )

    @Test fun path_unlocks_in_order() {
        val first = CoursePath.build(course, Progress()).flatMap { it.nodes }
        assertEquals(NodeStatus.CURRENT, first[0].status)
        assertTrue(first.drop(1).all { it.status == NodeStatus.LOCKED })

        val p = Progress(lessons = mapOf("ns-1" to 80))
        assertEquals("ns-2", CoursePath.current(course, p)?.lesson?.id)
        assertTrue(CoursePath.isPlayable(course, p, "ns-1"))
        assertFalse(CoursePath.isPlayable(course, p, "ds-1"))
    }

    @Test fun finishing_a_unit_unlocks_its_case_studies() {
        assertTrue(CoursePath.unlockedCaseStudies(course, Progress(lessons = mapOf("ns-1" to 100))).isEmpty())
        val done = Progress(lessons = mapOf("ns-1" to 100, "ns-2" to 75))
        assertEquals(listOf("Swiggy", "Zomato"), CoursePath.unlockedCaseStudies(course, done).map { it.company })
    }

    @Test fun lesson_result_updates_xp_streak_best_and_deck() {
        val lesson = course.lesson("ns-1")!!
        val ids = lesson.exercises.map { it.id }
        val p = Progress(lessons = mapOf("ns-1" to 100))
            .withResult(result("ns-1", missed = setOf(ids[0])).copy(accuracyPercent = 80), ids, today)
        assertEquals(15, p.totalXp)
        assertEquals(15, p.xpOn(today))
        assertEquals(1, p.streak.current)
        assertEquals("keeps the best score", 100, p.lessons["ns-1"])
        assertEquals(ids.size, p.deck.size)
        assertEquals(1, p.deck[ids[0]]?.box)
    }

    @Test fun old_history_is_trimmed() {
        val old = today.minusDays(60)
        val p = Progress(dailyXp = mapOf(old to 30)).withResult(result("ns-1"), emptyList(), today)
        assertFalse(old in p.dailyXp)
    }

    @Test fun codec_round_trips() {
        val p = Progress(
            totalXp = 135,
            dailyGoalXp = 30,
            hearts = Hearts(3, 1_764_230_400_000),
            streak = Streak(3, 9, today, 1),
            lessons = mapOf("ns-1" to 80, "ns-2" to 100),
            dailyXp = mapOf(today to 25, today.minusDays(1) to 10),
            deck = mapOf("ns-1-2" to ReviewCard("ns-1-2", 1, today.plusDays(1))),
        )
        assertEquals(p, ProgressCodec.decode(ProgressCodec.encode(p)))
        assertEquals(Progress(), ProgressCodec.decode(ProgressCodec.encode(Progress())))
    }

    @Test fun codec_skips_bad_lines() {
        val p = ProgressCodec.decode("v=1\nxp=oops\nlesson=ns-1:90\ncard=broken\nfuture=1\nday=2026-11-27:12")
        assertEquals(0, p.totalXp)
        assertEquals(mapOf("ns-1" to 90), p.lessons)
        assertEquals(12, p.xpOn(today))
        assertTrue(p.deck.isEmpty())
        assertEquals(Progress(), ProgressCodec.decode(null))
    }
}

class HighlighterTest {
    private fun kinds(code: String) = KotlinHighlighter.tokenize(code).map { it.kind to code.substring(it.start, it.end) }

    @Test fun highlights_keywords_types_strings_and_null_ops() {
        assertEquals(
            listOf(
                Kind.KEYWORD to "val", Kind.TYPE to "String", Kind.NULL_OP to "?.", Kind.NULL_OP to "?:",
                Kind.NUMBER to "0",
            ),
            kinds("val len: String = name?.length ?: 0"),
        )
        assertEquals(listOf(Kind.FUNCTION to "println", Kind.STRING to "\"a \\\" b\""), kinds("println(\"a \\\" b\")"))
    }

    @Test fun comments_annotations_and_lambdas() {
        assertEquals(
            listOf(Kind.ANNOTATION to "@Composable", Kind.KEYWORD to "fun", Kind.TYPE to "Card", Kind.COMMENT to "// hi"),
            kinds("@Composable fun Card() // hi"),
        )
        assertEquals(listOf(Kind.FUNCTION to "launch", Kind.FUNCTION to "delay", Kind.NUMBER to "100"), kinds("launch { delay(100) }"))
        assertEquals(listOf(Kind.NUMBER to "1.5f", Kind.NUMBER to "0", Kind.NUMBER to "10"), kinds("x = 1.5f; 0..10"))
    }

    @Test fun unterminated_string_stops_at_line_end() {
        val code = "val s = \"oops\nval t = 1"
        val spans = KotlinHighlighter.tokenize(code)
        assertEquals("\"oops", code.substring(spans[1].start, spans[1].end))
        assertEquals(Kind.KEYWORD, spans[2].kind)
    }
}
