package com.gayatripatel.kotlinquest

import android.app.Application
import com.gayatripatel.kotlinquest.core.Course
import com.gayatripatel.kotlinquest.core.CourseContent
import com.gayatripatel.kotlinquest.data.Clock
import com.gayatripatel.kotlinquest.data.ProgressRepository
import com.gayatripatel.kotlinquest.data.SystemClock
import com.gayatripatel.kotlinquest.ui.lesson.LessonViewModel
import com.gayatripatel.kotlinquest.ui.path.PathViewModel
import com.gayatripatel.kotlinquest.ui.profile.ProfileViewModel
import com.gayatripatel.kotlinquest.ui.review.ReviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single<Clock> { SystemClock }
    single<Course> { CourseContent.course }
    single { ProgressRepository(androidContext(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }

    viewModel { PathViewModel(get(), get(), get()) }
    viewModel { ReviewViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { params -> LessonViewModel(params.get(), get(), get(), get()) }
}

class QuestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QuestApp)
            modules(appModule)
        }
    }
}
