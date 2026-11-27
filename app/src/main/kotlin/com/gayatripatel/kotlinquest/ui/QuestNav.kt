package com.gayatripatel.kotlinquest.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gayatripatel.kotlinquest.core.LessonSession
import com.gayatripatel.kotlinquest.ui.lesson.LessonRoute
import com.gayatripatel.kotlinquest.ui.path.PathRoute
import com.gayatripatel.kotlinquest.ui.profile.ProfileRoute
import com.gayatripatel.kotlinquest.ui.review.ReviewRoute

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    LEARN("learn", "Learn", Icons.Filled.School),
    REVIEW("review", "Review", Icons.Filled.Replay),
    PROFILE("profile", "Profile", Icons.Filled.Person),
}

@Composable
fun TabShell(selected: Tab?, onSelect: (Tab) -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selected != null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@Composable
fun QuestNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tab = Tab.entries.firstOrNull { it.route == entry?.destination?.route }

    fun openTab(t: Tab) = nav.navigate(t.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun openLesson(id: String) = nav.navigate("lesson/$id")

    TabShell(tab, ::openTab) { modifier ->
        NavHost(nav, startDestination = Tab.LEARN.route, modifier = modifier) {
            composable(Tab.LEARN.route) {
                PathRoute(onStartLesson = ::openLesson, onReview = { openLesson(LessonSession.REVIEW_ID) })
            }
            composable(Tab.REVIEW.route) { ReviewRoute(onStart = { openLesson(LessonSession.REVIEW_ID) }) }
            composable(Tab.PROFILE.route) { ProfileRoute() }
            composable("lesson/{id}") { backStack ->
                LessonRoute(
                    lessonId = backStack.arguments?.getString("id").orEmpty(),
                    onClose = { nav.popBackStack() },
                    onReview = {
                        nav.popBackStack()
                        openLesson(LessonSession.REVIEW_ID)
                    },
                )
            }
        }
    }
}
