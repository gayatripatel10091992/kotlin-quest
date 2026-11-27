package com.gayatripatel.kotlinquest.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gayatripatel.kotlinquest.core.Answer
import com.gayatripatel.kotlinquest.core.CaseStudy
import com.gayatripatel.kotlinquest.core.ChoiceKind
import com.gayatripatel.kotlinquest.core.CodeFormat
import com.gayatripatel.kotlinquest.core.Course
import com.gayatripatel.kotlinquest.core.CoursePath
import com.gayatripatel.kotlinquest.core.Exercise
import com.gayatripatel.kotlinquest.core.Hearts
import com.gayatripatel.kotlinquest.core.LessonResult
import com.gayatripatel.kotlinquest.core.LessonSession
import com.gayatripatel.kotlinquest.core.LessonState
import com.gayatripatel.kotlinquest.core.Leitner
import com.gayatripatel.kotlinquest.core.reviewed
import com.gayatripatel.kotlinquest.core.solutionText
import com.gayatripatel.kotlinquest.core.withResult
import com.gayatripatel.kotlinquest.data.Clock
import com.gayatripatel.kotlinquest.data.ProgressRepository
import com.gayatripatel.kotlinquest.ui.MviViewModel
import com.gayatripatel.kotlinquest.ui.components.CodeBlock
import com.gayatripatel.kotlinquest.ui.components.CodeStyle
import com.gayatripatel.kotlinquest.ui.components.RichText
import com.gayatripatel.kotlinquest.ui.components.StatChip
import com.gayatripatel.kotlinquest.ui.components.formatDuration
import com.gayatripatel.kotlinquest.ui.theme.Danger
import com.gayatripatel.kotlinquest.ui.theme.DangerContainer
import com.gayatripatel.kotlinquest.ui.theme.Flame
import com.gayatripatel.kotlinquest.ui.theme.HeartRed
import com.gayatripatel.kotlinquest.ui.theme.KotlinOrange
import com.gayatripatel.kotlinquest.ui.theme.KotlinPink
import com.gayatripatel.kotlinquest.ui.theme.KotlinPurple
import com.gayatripatel.kotlinquest.ui.theme.Success
import com.gayatripatel.kotlinquest.ui.theme.SuccessContainer
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** What the learner has picked but not yet checked. Word-bank picks are bank indexes, so repeated tokens work. */
data class Selection(val option: Int? = null, val tokens: List<Int> = emptyList())

data class LessonUiState(
    val isReview: Boolean = false,
    val title: String = "",
    val session: LessonState? = null,
    val selection: Selection = Selection(),
    val missing: Boolean = false,
    val minutesToHeart: Int? = null,
    val streakAfter: Int? = null,
    val unlocked: List<CaseStudy> = emptyList(),
)

sealed interface LessonIntent {
    data class SelectOption(val index: Int) : LessonIntent
    data class TapBank(val bankIndex: Int) : LessonIntent
    data class RemoveToken(val position: Int) : LessonIntent
    data object Check : LessonIntent
    data object Continue : LessonIntent
}

fun answerFor(exercise: Exercise, selection: Selection): Answer? = when (exercise) {
    is Exercise.ArrangeTokens -> if (selection.tokens.isEmpty()) null else {
        val bank = exercise.bank()
        Answer.Tokens(selection.tokens.map { bank[it] })
    }
    else -> selection.option?.let { Answer.Option(it) }
}

fun minutesToHeart(hearts: Hearts, nowMillis: Long): Int? =
    hearts.millisToNext(nowMillis)?.let { ((it + 59_999) / 60_000).toInt() }

class LessonViewModel(
    private val lessonId: String,
    private val repo: ProgressRepository,
    private val course: Course,
    private val clock: Clock,
) : MviViewModel<LessonUiState, LessonIntent>(LessonUiState(isReview = lessonId == LessonSession.REVIEW_ID)) {

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        val progress = repo.current()
        val now = clock.now()
        if (lessonId == LessonSession.REVIEW_ID) {
            val exercises = Leitner.due(progress.deck, clock.today()).mapNotNull { course.exercise(it.exerciseId) }
            if (exercises.isEmpty()) {
                setState { copy(missing = true) }
            } else {
                setState { copy(session = LessonSession.startReview(exercises, progress.hearts, now), title = "Review") }
            }
            return
        }
        val lesson = course.lesson(lessonId)
        if (lesson == null || !CoursePath.isPlayable(course, progress, lessonId)) {
            setState { copy(missing = true) }
            return
        }
        val session = LessonSession.start(lesson, progress.hearts, now)
        setState { copy(session = session, title = lesson.title, minutesToHeart = minutesToHeart(session.hearts, now)) }
    }

    override fun onIntent(intent: LessonIntent) {
        val session = currentState.session ?: return
        val answering = session.phase == LessonState.Phase.Answering
        when (intent) {
            is LessonIntent.SelectOption -> if (answering) setState { copy(selection = Selection(option = intent.index)) }
            is LessonIntent.TapBank -> if (answering && intent.bankIndex !in currentState.selection.tokens) {
                setState { copy(selection = selection.copy(tokens = selection.tokens + intent.bankIndex)) }
            }
            is LessonIntent.RemoveToken -> if (answering) {
                setState { copy(selection = selection.copy(tokens = selection.tokens.filterIndexed { i, _ -> i != intent.position })) }
            }
            LessonIntent.Check -> check(session)
            LessonIntent.Continue -> proceed(session)
        }
    }

    private fun check(session: LessonState) {
        val exercise = session.current ?: return
        val answer = answerFor(exercise, currentState.selection) ?: return
        val next = LessonSession.answer(session, answer, clock.now())
        setState { copy(session = next) }
        val feedback = next.phase as? LessonState.Phase.Feedback ?: return
        viewModelScope.launch {
            when {
                !next.usesHearts -> repo.update { it.reviewed(exercise.id, feedback.correct, clock.today()) }
                !feedback.correct -> repo.update { it.copy(hearts = next.hearts) } // a lost heart sticks, even if they quit
                else -> Unit
            }
        }
    }

    private fun proceed(session: LessonState) {
        val now = clock.now()
        val next = LessonSession.proceed(session, now)
        setState { copy(session = next, selection = Selection(), minutesToHeart = minutesToHeart(next.hearts, now)) }
        val complete = next.phase as? LessonState.Phase.Complete ?: return
        viewModelScope.launch {
            val ids = course.lesson(lessonId)?.exercises?.map { it.id }.orEmpty()
            val today = clock.today()
            var newlyUnlocked = emptyList<CaseStudy>()
            var streak = 0
            repo.update { before ->
                val after = before.withResult(complete.result, ids, today)
                newlyUnlocked = CoursePath.unlockedCaseStudies(course, after) - CoursePath.unlockedCaseStudies(course, before).toSet()
                streak = after.streak.display(today)
                after
            }
            setState { copy(streakAfter = streak, unlocked = newlyUnlocked) }
        }
    }
}

@Composable
fun LessonRoute(lessonId: String, onClose: () -> Unit, onReview: () -> Unit) {
    val viewModel: LessonViewModel = koinViewModel(parameters = { parametersOf(lessonId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    LessonScreen(state, viewModel::onIntent, onClose, onReview)
}

@Composable
fun LessonScreen(state: LessonUiState, onIntent: (LessonIntent) -> Unit, onClose: () -> Unit, onReview: () -> Unit) {
    val session = state.session
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.missing -> MissingContent(state.isReview, onClose)
            session == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> when (val phase = session.phase) {
                is LessonState.Phase.Complete -> CompleteContent(phase.result, state, onClose)
                LessonState.Phase.OutOfHearts -> OutOfHeartsContent(state.minutesToHeart, onReview, onClose)
                else -> ExerciseContent(session, state.selection, onIntent, onClose)
            }
        }
    }
}

private fun kindLabel(exercise: Exercise): String = when (exercise) {
    is Exercise.Choice -> exercise.kind.label
    is Exercise.FillBlank -> "Fill the gap"
    is Exercise.ArrangeTokens -> "Build the line"
}

private fun kindColor(exercise: Exercise): Color = when (exercise) {
    is Exercise.Choice -> when (exercise.kind) {
        ChoiceKind.CONCEPT -> KotlinPurple
        ChoiceKind.PREDICT_OUTPUT -> Color(0xFF3D7BFD)
        ChoiceKind.FIX_THE_BUG -> KotlinPink
    }
    is Exercise.FillBlank -> KotlinOrange
    is Exercise.ArrangeTokens -> Success
}

@Composable
private fun ExerciseContent(session: LessonState, selection: Selection, onIntent: (LessonIntent) -> Unit, onClose: () -> Unit) {
    val exercise = session.current ?: return
    val feedback = session.phase as? LessonState.Phase.Feedback
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Quit lesson") }
            LinearProgressIndicator(
                progress = { session.progress },
                modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)),
                color = Success,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                drawStopIndicator = {},
            )
            Spacer(Modifier.width(14.dp))
            if (session.usesHearts) StatChip(Icons.Filled.Favorite, "${session.hearts.count}", HeartRed)
            else StatChip(Icons.Filled.Replay, "Review", KotlinPurple)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            if (feedback?.correct == true && session.combo >= 3) {
                Text(
                    "${session.combo} IN A ROW!",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = KotlinOrange,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(kindLabel(exercise).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = kindColor(exercise))
            Spacer(Modifier.height(6.dp))
            RichText(exercise.prompt, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(18.dp))
            val locked = feedback != null
            when (exercise) {
                is Exercise.Choice -> ChoiceBody(exercise, selection.option, feedback, locked) { onIntent(LessonIntent.SelectOption(it)) }
                is Exercise.FillBlank -> FillBlankBody(exercise, selection.option, feedback, locked) { onIntent(LessonIntent.SelectOption(it)) }
                is Exercise.ArrangeTokens -> ArrangeBody(
                    exercise, selection.tokens, locked,
                    onTap = { onIntent(LessonIntent.TapBank(it)) },
                    onRemove = { onIntent(LessonIntent.RemoveToken(it)) },
                )
            }
        }

        BottomBar(
            exercise = exercise,
            feedback = feedback,
            canCheck = answerFor(exercise, selection) != null,
            onCheck = { onIntent(LessonIntent.Check) },
            onContinue = { onIntent(LessonIntent.Continue) },
        )
    }
}

private enum class Mark { NONE, SELECTED, CORRECT, WRONG }

private fun markFor(index: Int, selected: Int?, correctIndex: Int, feedback: LessonState.Phase.Feedback?): Mark = when {
    feedback != null && index == correctIndex -> Mark.CORRECT
    feedback != null && index == selected -> Mark.WRONG
    index == selected -> Mark.SELECTED
    else -> Mark.NONE
}

@Composable
private fun markColor(mark: Mark): Color = when (mark) {
    Mark.NONE -> MaterialTheme.colorScheme.outlineVariant
    Mark.SELECTED -> MaterialTheme.colorScheme.primary
    Mark.CORRECT -> Success
    Mark.WRONG -> Danger
}

@Composable
private fun ChoiceBody(
    exercise: Exercise.Choice,
    selected: Int?,
    feedback: LessonState.Phase.Feedback?,
    locked: Boolean,
    onSelect: (Int) -> Unit,
) {
    exercise.code?.let {
        CodeBlock(it, highlightLine = exercise.highlightLine)
        Spacer(Modifier.height(18.dp))
    }
    exercise.options.forEachIndexed { i, option ->
        val mark = markFor(i, selected, exercise.correctIndex, feedback)
        val color = markColor(mark)
        Surface(
            onClick = { onSelect(i) },
            enabled = !locked,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, color),
            color = if (mark == Mark.NONE) MaterialTheme.colorScheme.surface else color.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).border(2.dp, color, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(('A' + i).toString(), fontWeight = FontWeight.Bold, color = if (mark == Mark.NONE) MaterialTheme.colorScheme.onSurfaceVariant else color)
                }
                Spacer(Modifier.width(14.dp))
                val style = if (exercise.kind == ChoiceKind.PREDICT_OUTPUT) CodeStyle.copy(fontSize = 16.sp) else MaterialTheme.typography.bodyLarge
                RichText(option, style, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillBlankBody(
    exercise: Exercise.FillBlank,
    selected: Int?,
    feedback: LessonState.Phase.Feedback?,
    locked: Boolean,
    onSelect: (Int) -> Unit,
) {
    val start = exercise.code.indexOf(Exercise.FillBlank.BLANK)
    val length = selected?.let { exercise.options[it].length } ?: Exercise.FillBlank.BLANK.length
    CodeBlock(exercise.filled(selected), emphasis = start until start + length)
    Spacer(Modifier.height(22.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        exercise.options.forEachIndexed { i, option ->
            TokenChip(option, markColor(markFor(i, selected, exercise.correctIndex, feedback)), enabled = !locked) { onSelect(i) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArrangeBody(
    exercise: Exercise.ArrangeTokens,
    chosen: List<Int>,
    locked: Boolean,
    onTap: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val bank = remember(exercise.id) { exercise.bank() }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .border(2.dp, lineColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        if (chosen.isEmpty()) {
            Text(
                "Tap the tokens below in order",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chosen.forEachIndexed { position, bankIndex ->
                TokenChip(bank[bankIndex], MaterialTheme.colorScheme.primary, enabled = !locked) { onRemove(position) }
            }
        }
    }
    if (chosen.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        CodeBlock(CodeFormat.joinTokens(chosen.map { bank[it] }))
    }
    Spacer(Modifier.height(22.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bank.forEachIndexed { i, token ->
            if (i in chosen) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Text(token, style = CodeStyle.copy(fontSize = 16.sp), color = Color.Transparent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            } else {
                TokenChip(token, MaterialTheme.colorScheme.outlineVariant, enabled = !locked) { onTap(i) }
            }
        }
    }
}

@Composable
private fun TokenChip(token: String, borderColor: Color, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(token, style = CodeStyle.copy(fontSize = 16.sp), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun BottomBar(
    exercise: Exercise,
    feedback: LessonState.Phase.Feedback?,
    canCheck: Boolean,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
) {
    val correct = feedback?.correct
    val background = when (correct) {
        true -> SuccessContainer
        false -> DangerContainer
        null -> MaterialTheme.colorScheme.background
    }
    val ink = when (correct) {
        true -> Color(0xFF0E5A24)
        false -> Color(0xFF8A1C1C)
        null -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(background)
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        if (correct != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null, tint = if (correct) Success else Danger, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (correct) "Nice!" else "Not quite", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
            }
            if (!correct) {
                Spacer(Modifier.height(8.dp))
                Text("Correct answer:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ink)
                if (exercise is Exercise.Choice) {
                    RichText(exercise.solutionText(), MaterialTheme.typography.bodyLarge, color = ink)
                } else {
                    Text(exercise.solutionText(), style = CodeStyle.copy(fontSize = 16.sp), color = ink)
                }
            }
            Spacer(Modifier.height(8.dp))
            RichText(exercise.explanation, MaterialTheme.typography.bodyMedium, color = ink)
            Spacer(Modifier.height(14.dp))
        }
        Button(
            onClick = if (feedback == null) onCheck else onContinue,
            enabled = feedback != null || canCheck,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (correct) {
                    true -> Success
                    false -> Danger
                    null -> MaterialTheme.colorScheme.primary
                },
            ),
        ) {
            Text(if (feedback == null) "CHECK" else "CONTINUE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun CompleteContent(result: LessonResult, state: LessonUiState, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Filled.EmojiEvents, null, tint = KotlinOrange, modifier = Modifier.size(112.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                result.isReview -> "Review complete!"
                result.perfect -> "Perfect lesson!"
                else -> "Lesson complete!"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KotlinOrange,
        )
        Text(state.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultTile("TOTAL XP", "+${result.xp}", KotlinOrange, Modifier.weight(1f))
            ResultTile("ACCURACY", "${result.accuracyPercent}%", Success, Modifier.weight(1f))
            ResultTile("TIME", formatDuration(result.durationMillis), Color(0xFF3D7BFD), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        if (result.bestCombo >= 3) {
            Text("Best combo: ${result.bestCombo} in a row", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        state.streakAfter?.let { streak ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Flame)
                Spacer(Modifier.width(6.dp))
                Text("$streak-day streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Flame)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (state.unlocked.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    "Unit complete! New in your profile: ${state.unlocked.joinToString { it.company }}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("CONTINUE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun ResultTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(color),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 6.dp))
        Box(
            Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, bottom = 3.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun OutOfHeartsContent(minutesToHeart: Int?, onReview: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.HeartBroken, null, tint = HeartRed, modifier = Modifier.size(112.dp))
        Spacer(Modifier.height(16.dp))
        Text("You're out of hearts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                if (minutesToHeart != null) append("Your next heart arrives in $minutesToHeart min. ")
                append("Hearts refill one every 30 minutes, and review practice never costs any.")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Filled.Replay, null)
            Spacer(Modifier.width(8.dp))
            Text("PRACTISE FOR FREE", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onClose) { Text("Back to the path") }
    }
}

@Composable
private fun MissingContent(isReview: Boolean, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (isReview) "Nothing to review right now" else "This lesson is still locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isReview) "Cards come back when they're due. Finish a lesson to add more."
            else "Finish the lessons before it on the path first.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose) { Text("Back") }
    }
}
