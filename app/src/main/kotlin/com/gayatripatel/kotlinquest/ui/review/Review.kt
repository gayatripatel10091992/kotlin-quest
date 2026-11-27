package com.gayatripatel.kotlinquest.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gayatripatel.kotlinquest.core.ChoiceKind
import com.gayatripatel.kotlinquest.core.Course
import com.gayatripatel.kotlinquest.core.Exercise
import com.gayatripatel.kotlinquest.core.Leitner
import com.gayatripatel.kotlinquest.core.Progress
import com.gayatripatel.kotlinquest.data.Clock
import com.gayatripatel.kotlinquest.data.ProgressRepository
import com.gayatripatel.kotlinquest.ui.components.RichText
import com.gayatripatel.kotlinquest.ui.path.minuteTicks
import com.gayatripatel.kotlinquest.ui.theme.KotlinPink
import com.gayatripatel.kotlinquest.ui.theme.KotlinPurple
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

data class ReviewItem(val id: String, val unit: String, val prompt: String, val box: Int)

data class ReviewUiState(
    val due: Int = 0,
    val total: Int = 0,
    val boxes: List<Int> = List(Leitner.MAX_BOX) { 0 },
    val upNext: List<ReviewItem> = emptyList(),
    val nextDueInDays: Long? = null,
)

fun reviewUiState(course: Course, progress: Progress, today: LocalDate): ReviewUiState {
    val due = Leitner.due(progress.deck, today)
    return ReviewUiState(
        due = Leitner.dueCount(progress.deck, today),
        total = progress.deck.size,
        boxes = Leitner.boxCounts(progress.deck),
        upNext = due.mapNotNull { card ->
            val exercise = course.exercise(card.exerciseId) ?: return@mapNotNull null
            val unit = course.units.firstOrNull { u -> u.lessons.any { l -> l.exercises.any { it.id == card.exerciseId } } }
            ReviewItem(card.exerciseId, unit?.title.orEmpty(), exercise.cardTitle(), card.box)
        },
        nextDueInDays = progress.deck.values.map { it.due }.filter { it.isAfter(today) }.minOrNull()
            ?.let { ChronoUnit.DAYS.between(today, it) },
    )
}

/** A short title for a card in the list: the prompt, or for "what does this print?" the first line of code. */
private fun Exercise.cardTitle(): String {
    val code = (this as? Exercise.Choice)?.takeIf { it.kind == ChoiceKind.PREDICT_OUTPUT }?.code ?: return prompt
    return "`${code.lineSequence().last().trim()}`"
}

class ReviewViewModel(repo: ProgressRepository, course: Course, clock: Clock) : ViewModel() {
    val uiState: StateFlow<ReviewUiState> = combine(repo.progress, minuteTicks()) { progress, _ ->
        reviewUiState(course, progress, clock.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())
}

@Composable
fun ReviewRoute(onStart: () -> Unit, viewModel: ReviewViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewScreen(state, onStart)
}

private val boxNames = listOf("New", "Learning", "Familiar", "Strong", "Mastered")

@Composable
fun ReviewScreen(state: ReviewUiState, onStart: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Review", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Spaced repetition: right answers come back later, misses come back sooner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { DueCard(state, onStart) }
        if (state.total > 0) {
            item { StrengthCard(state.boxes) }
        }
        if (state.upNext.isNotEmpty()) {
            item { Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.upNext, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.unit.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            RichText(item.prompt, MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(boxNames[item.box - 1], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DueCard(state: ReviewUiState, onStart: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(KotlinPurple, KotlinPink)))
            .padding(20.dp),
    ) {
        Column {
            Icon(if (state.total == 0) Icons.Filled.School else Icons.Filled.Replay, null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    state.total == 0 -> "No cards yet"
                    state.due == 0 -> "All caught up"
                    state.due == 1 -> "1 card is due"
                    else -> "${state.due} cards are due"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when {
                    state.total == 0 -> "Every lesson you finish adds its cards to your deck."
                    state.due == 0 && state.nextDueInDays != null ->
                        "Next card is due in ${state.nextDueInDays} day${if (state.nextDueInDays == 1L) "" else "s"}."
                    else -> "Up to ${Leitner.SESSION_SIZE} per session, weakest first. Reviews never cost hearts."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            if (state.due > 0) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = KotlinPurple),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("START REVIEW", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun StrengthCard(boxes: List<Int>) {
    val max = (boxes.maxOrNull() ?: 0).coerceAtLeast(1)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Memory strength", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${boxes.sum()} cards across five Leitner boxes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                boxes.forEachIndexed { i, count ->
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((6 + 74 * count / max).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(KotlinPurple.copy(alpha = 0.35f + 0.13f * i)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                boxNames.forEach {
                    Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
