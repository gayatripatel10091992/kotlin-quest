package com.gayatripatel.kotlinquest.core

/** What kind of multiple-choice card this is. The UI labels and styles each one differently. */
enum class ChoiceKind(val label: String) {
    CONCEPT("Pick the right answer"),
    PREDICT_OUTPUT("What does this print?"),
    FIX_THE_BUG("Spot the bug"),
}

/** One card in a lesson. Every exercise can grade an [Answer] and explain itself afterwards. */
sealed interface Exercise {
    val id: String
    val prompt: String
    val explanation: String

    /** Answer the [prompt] by picking one of [options]. [code] and [highlightLine] (1-based) are optional context. */
    data class Choice(
        override val id: String,
        val kind: ChoiceKind,
        override val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String,
        val code: String? = null,
        val highlightLine: Int? = null,
    ) : Exercise {
        init {
            require(options.size >= 2) { "$id needs at least two options" }
            require(correctIndex in options.indices) { "$id: correctIndex out of range" }
        }
    }

    /** Duolingo's word bank, for code: tap tokens into order. Distractors are mixed into the bank. */
    data class ArrangeTokens(
        override val id: String,
        override val prompt: String,
        val solution: List<String>,
        val distractors: List<String>,
        override val explanation: String,
        val acceptedAlternatives: List<List<String>> = emptyList(),
    ) : Exercise {
        init {
            require(solution.isNotEmpty()) { "$id needs a solution" }
        }

        /** The shuffled bank. Seeded by the exercise id, so it's stable across recompositions and screenshots. */
        fun bank(): List<String> = (solution + distractors).shuffled(kotlin.random.Random(id.hashCode()))
    }

    /** A snippet with one `___` gap and a few tokens to fill it with. */
    data class FillBlank(
        override val id: String,
        override val prompt: String,
        val code: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String,
    ) : Exercise {
        init {
            require(code.split(BLANK).size == 2) { "$id needs exactly one $BLANK gap" }
            require(correctIndex in options.indices) { "$id: correctIndex out of range" }
        }

        fun filled(index: Int?): String = code.replace(BLANK, index?.let { options[it] } ?: BLANK)

        companion object {
            const val BLANK = "___"
        }
    }
}

sealed interface Answer {
    data class Option(val index: Int) : Answer
    data class Tokens(val tokens: List<String>) : Answer
}

/** Grades an answer. Answers of the wrong shape for the exercise are simply wrong, never a crash. */
fun Exercise.isCorrect(answer: Answer): Boolean = when (this) {
    is Exercise.Choice -> answer is Answer.Option && answer.index == correctIndex
    is Exercise.FillBlank -> answer is Answer.Option && answer.index == correctIndex
    is Exercise.ArrangeTokens -> answer is Answer.Tokens &&
        (answer.tokens == solution || acceptedAlternatives.any { it == answer.tokens })
}

/** The correct answer as text, shown in the "Correct solution" sheet after a miss. */
fun Exercise.solutionText(): String = when (this) {
    is Exercise.Choice -> options[correctIndex]
    is Exercise.FillBlank -> filled(correctIndex)
    is Exercise.ArrangeTokens -> CodeFormat.joinTokens(solution)
}

data class Lesson(val id: String, val title: String, val exercises: List<Exercise>)

/** A real company that ships Kotlin, unlocked by finishing the unit it illustrates. */
data class CaseStudy(val company: String, val category: String, val story: String)

data class CourseUnit(
    val id: String,
    val title: String,
    val subtitle: String,
    val lessons: List<Lesson>,
    val caseStudies: List<CaseStudy>,
)

data class Course(val units: List<CourseUnit>) {
    val lessons: List<Lesson> get() = units.flatMap { it.lessons }
    private val exerciseIndex: Map<String, Exercise> by lazy { lessons.flatMap { it.exercises }.associateBy { it.id } }

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }
    fun exercise(id: String): Exercise? = exerciseIndex[id]
    fun unitOf(lessonId: String): CourseUnit? = units.firstOrNull { u -> u.lessons.any { it.id == lessonId } }
}

object CodeFormat {
    private val noSpaceBefore = setOf(".", "?.", ",", ")", "(", "()", ">", ":", "!!")
    private val noSpaceAfter = setOf(".", "?.", "(", "<", "!!")

    /**
     * Joins word-bank tokens the way a person would type them: `items.sumOf { it.price }`, not
     * `items . sumOf { it . price }`. A colon keeps its space after (`title: String`).
     */
    fun joinTokens(tokens: List<String>): String = buildString {
        tokens.forEachIndexed { i, token ->
            if (i > 0) {
                val prev = tokens[i - 1]
                val tight = token in noSpaceBefore || prev in noSpaceAfter || (token == "<" && prev.first().isUpperCase())
                if (!tight) append(' ')
            }
            append(token)
        }
    }
}
