package jez.jetpackracer.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun <T, R> MutableStateFlow<T>.toViewState(scope: CoroutineScope, mapper: (T) -> R) =
    map(mapper)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            mapper(this.value)
        )

fun <T, R> Flow<T>.toViewState(scope: CoroutineScope, initial: T, mapper: (T) -> R) =
    map(mapper)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            mapper(initial)
        )
