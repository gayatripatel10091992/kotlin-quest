package com.gayatripatel.kotlinquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gayatripatel.kotlinquest.core.CaseStudy
import com.gayatripatel.kotlinquest.core.Course
import com.gayatripatel.kotlinquest.core.CoursePath
import com.gayatripatel.kotlinquest.core.Progress
import com.gayatripatel.kotlinquest.data.Clock
import com.gayatripatel.kotlinquest.data.ProgressRepository
import com.gayatripatel.kotlinquest.ui.path.minuteTicks
import com.gayatripatel.kotlinquest.ui.theme.Flame
import com.gayatripatel.kotlinquest.ui.theme.KotlinOrange
import com.gayatripatel.kotlinquest.ui.theme.KotlinPink
import com.gayatripatel.kotlinquest.ui.theme.KotlinPurple
import com.gayatripatel.kotlinquest.ui.theme.Success
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class DayCell(val date: LocalDate, val xp: Int, val isToday: Boolean)

data class LockedStories(val unitTitle: String, val count: Int)

data class ProfileUiState(
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val freezes: Int = 0,
    val xp: Int = 0,
    val lessonsDone: Int = 0,
    val lessonsTotal: Int = 0,
    val goal: Int = Progress.DEFAULT_GOAL,
    val weeks: List<List<DayCell>> = emptyList(),
    val unlocked: List<CaseStudy> = emptyList(),
    val locked: List<LockedStories> = emptyList(),
)

const val ACTIVITY_WEEKS = 5

fun profileUiState(course: Course, progress: Progress, today: LocalDate): ProfileUiState {
    val units = CoursePath.build(course, progress)
    val days = (ACTIVITY_WEEKS * 7 - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        DayCell(date, progress.xpOn(date), date == today)
    }
    return ProfileUiState(
        streak = progress.streak.display(today),
        longestStreak = progress.streak.longest,
        freezes = progress.streak.freezes,
        xp = progress.totalXp,
        lessonsDone = progress.lessons.size,
        lessonsTotal = course.lessons.size,
        goal = progress.dailyGoalXp,
        weeks = days.chunked(7),
        unlocked = units.filter { it.completed }.flatMap { it.unit.caseStudies },
        locked = units.filterNot { it.completed }.map { LockedStories(it.unit.title, it.unit.caseStudies.size) },
    )
}

class ProfileViewModel(private val repo: ProgressRepository, course: Course, clock: Clock) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = combine(repo.progress, minuteTicks()) { progress, _ ->
        profileUiState(course, progress, clock.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setGoal(xp: Int) {
        viewModelScope.launch { repo.update { it.copy(dailyGoalXp = xp) } }
    }
}

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state, viewModel::setGoal)
}

private val storyColors = listOf(KotlinPurple, KotlinPink, KotlinOrange, Color(0xFF3D7BFD), Success)

@Composable
fun ProfileScreen(state: ProfileUiState, onGoalChange: (Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Your progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Filled.LocalFireDepartment, Flame, "${state.streak}", "Day streak · best ${state.longestStreak}", Modifier.weight(1f))
                    StatTile(Icons.Filled.Bolt, KotlinPurple, "${state.xp}", "Total XP", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Filled.School, Success, "${state.lessonsDone}/${state.lessonsTotal}", "Lessons done", Modifier.weight(1f))
                    StatTile(Icons.Filled.AcUnit, Color(0xFF3DB9F5), "${state.freezes}", "Streak freezes", Modifier.weight(1f))
                }
            }
        }
        item { ActivityCard(state) }
        item { GoalCard(state.goal, onGoalChange) }
        item {
            Column {
                Text("Kotlin in production", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Finish a unit to unlock how real teams use what you just learned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.unlocked, key = { it.company }) { story ->
            StoryCard(story, storyColors[state.unlocked.indexOf(story) % storyColors.size])
        }
        items(state.locked, key = { "locked-${it.unitTitle}" }) { locked ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Finish “${locked.unitTitle}” to unlock ${locked.count} ${if (locked.count == 1) "story" else "stories"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActivityCard(state: ProfileUiState) {
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    val primary = MaterialTheme.colorScheme.primary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Last $ACTIVITY_WEEKS weeks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Full squares hit the daily goal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.weeks.forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { day ->
                            val color = when {
                                day.xp == 0 -> empty
                                day.xp >= state.goal -> primary
                                else -> primary.copy(alpha = 0.4f)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day.isToday) Box(Modifier.size(6.dp).clip(CircleShape).background(KotlinOrange))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Int, onGoalChange: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Daily goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Progress.GOALS.forEach { option ->
                    FilterChip(
                        selected = option == goal,
                        onClick = { onGoalChange(option) },
                        label = { Text("$option XP") },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCard(story: CaseStudy, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Text(story.company.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(story.company, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(story.category, style = MaterialTheme.typography.labelMedium, color = color)
                Spacer(Modifier.height(4.dp))
                Text(story.story, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
