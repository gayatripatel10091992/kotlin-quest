package com.gayatripatel.kotlinquest.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A tiny MVI base, in the spirit of Airbnb's Mavericks: the screen sends intents, the ViewModel reduces them
 * into one immutable state, and the UI renders that state and nothing else.
 */
abstract class MviViewModel<S : Any, I : Any>(initial: S) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S get() = _state.value

    protected fun setState(reducer: S.() -> S) = _state.update(reducer)

    abstract fun onIntent(intent: I)
}
