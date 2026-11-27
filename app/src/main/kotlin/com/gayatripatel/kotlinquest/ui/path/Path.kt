package com.gayatripatel.kotlinquest.ui.path

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.gayatripatel.kotlinquest.core.Course
import com.gayatripatel.kotlinquest.core.CoursePath
import com.gayatripatel.kotlinquest.core.Leitner
import com.gayatripatel.kotlinquest.core.NodeStatus
import com.gayatripatel.kotlinquest.core.PathNode
import com.gayatripatel.kotlinquest.core.Progress
import com.gayatripatel.kotlinquest.core.UnitProgress
import com.gayatripatel.kotlinquest.data.Clock
import com.gayatripatel.kotlinquest.data.ProgressRepository
import com.gayatripatel.kotlinquest.ui.components.TopStats
import com.gayatripatel.kotlinquest.ui.theme.Flame
import com.gayatripatel.kotlinquest.ui.theme.KotlinOrange
import com.gayatripatel.kotlinquest.ui.theme.KotlinPink
import com.gayatripatel.kotlinquest.ui.theme.KotlinPurple
import com.gayatripatel.kotlinquest.ui.theme.Locked
import com.gayatripatel.kotlinquest.ui.theme.Success
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

data class PathUiState(
    val units: List<UnitProgress> = emptyList(),
    val streak: Int = 0,
    val practisedToday: Boolean = false,
    val streakAtRisk: Boolean = false,
    val hearts: Int = 5,
    val xp: Int = 0,
    val todayXp: Int = 0,
    val goal: Int = Progress.DEFAULT_GOAL,
    val dueReviews: Int = 0,
)

fun pathUiState(course: Course, progress: Progress, nowMillis: Long, today: LocalDate) = PathUiState(
    units = CoursePath.build(course, progress),
    streak = progress.streak.display(today),
    practisedToday = progress.streak.practisedOn(today),
    streakAtRisk = progress.streak.atRisk(today),
    hearts = progress.hearts.current(nowMillis),
    xp = progress.totalXp,
    todayXp = progress.xpOn(today),
    goal = progress.dailyGoalXp,
    dueReviews = Leitner.dueCount(progress.deck, today),
)

/** Re-derives the path every minute too, so refilled hearts and a new day show up without a restart. */
fun minuteTicks() = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
}

class PathViewModel(repo: ProgressRepository, course: Course, clock: Clock) : ViewModel() {
    val uiState: StateFlow<PathUiState> = combine(repo.progress, minuteTicks()) { progress, _ ->
        pathUiState(course, progress, clock.now(), clock.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PathUiState())
}

@Composable
fun PathRoute(onStartLesson: (String) -> Unit, onReview: () -> Unit, viewModel: PathViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PathScreen(state, onStartLesson, onReview)
}

private val unitGradients = listOf(
    listOf(KotlinPurple, Color(0xFFB125EA)),
    listOf(Color(0xFFB125EA), KotlinPink),
    listOf(KotlinPink, KotlinOrange),
    listOf(Color(0xFF3D7BFD), KotlinPurple),
)

/** Horizontal offsets that make the lesson nodes snake down the screen. */
private val zigzag = listOf(0, 56, 0, -56)

/**
 * List index of the banner for the unit the learner is working on, so the path can open there. Stays at the top
 * (daily goal card) while they're still in unit 1. Mirrors the item order in [PathScreen].
 */
fun currentUnitItemIndex(units: List<UnitProgress>): Int {
    val current = units.indexOfFirst { !it.completed }
    if (current <= 0) return 0
    return 1 + units.take(current).sumOf { 1 + it.nodes.size + if (it.completed) 1 else 0 }
}

@Composable
fun PathScreen(state: PathUiState, onStartLesson: (String) -> Unit, onReview: () -> Unit) {
    val target = remember(state.units) { currentUnitItemIndex(state.units) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = target)
    LaunchedEffect(target) { if (target > 0) listState.scrollToItem(target) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopStats(state.streak, state.practisedToday, state.xp, state.hearts, Modifier.statusBarsPadding())
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DailyGoalCard(state, onReview) }
            state.units.forEachIndexed { unitIndex, unit ->
                item(key = unit.unit.id) { UnitBanner(unit, unitIndex) }
                items(unit.nodes, key = { it.lesson.id }) { node ->
                    val index = unit.nodes.indexOf(node) + unitIndex * 2
                    LessonNode(node, zigzag[index % zigzag.size], unitGradients[unitIndex % unitGradients.size].first(), onStartLesson)
                }
                if (unit.completed) {
                    item(key = "${unit.unit.id}-done") { UnlockedNote(unit) }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalCard(state: PathUiState, onReview: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            state.todayXp >= state.goal -> "Goal met. Nice work!"
                            state.streakAtRisk -> "Finish a lesson today to keep your ${state.streak}-day streak"
                            else -> "${state.goal - state.todayXp} XP to go today"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.streakAtRisk) Flame else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${state.todayXp}/${state.goal} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (state.todayXp.toFloat() / state.goal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = if (state.todayXp >= state.goal) Success else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                drawStopIndicator = {},
            )
            if (state.dueReviews > 0) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onReview, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${state.dueReviews} cards ready to review")
                }
            }
        }
    }
}

@Composable
private fun UnitBanner(unit: UnitProgress, index: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(unitGradients[index % unitGradients.size]))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("UNIT ${index + 1}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Text(unit.unit.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(unit.unit.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            Text(
                "${unit.completedCount}/${unit.nodes.size}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LessonNode(node: PathNode, offsetDp: Int, color: Color, onStart: (String) -> Unit) {
    val enabled = node.status != NodeStatus.LOCKED
    val fill = when (node.status) {
        NodeStatus.COMPLETED -> color
        NodeStatus.CURRENT -> color
        NodeStatus.LOCKED -> Locked
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.offset(x = offsetDp.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (node.status == NodeStatus.CURRENT) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, color)) {
                    Text(
                        "START",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            val size = if (node.status == NodeStatus.CURRENT) 84.dp else 72.dp
            Box(
                Modifier
                    .size(size)
                    .then(if (node.status == NodeStatus.CURRENT) Modifier.border(6.dp, color.copy(alpha = 0.25f), CircleShape) else Modifier)
                    .padding(if (node.status == NodeStatus.CURRENT) 6.dp else 0.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(enabled = enabled) { onStart(node.lesson.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (node.status) {
                        NodeStatus.COMPLETED -> Icons.Filled.Check
                        NodeStatus.CURRENT -> Icons.Filled.Star
                        NodeStatus.LOCKED -> Icons.Filled.Lock
                    },
                    contentDescription = node.lesson.title,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                node.lesson.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            node.bestAccuracy?.let {
                Text("Best $it%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UnlockedNote(unit: UnitProgress) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, null, tint = KotlinOrange)
            Spacer(Modifier.width(10.dp))
            Text(
                "Unlocked in your profile: ${unit.unit.caseStudies.joinToString { it.company }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
