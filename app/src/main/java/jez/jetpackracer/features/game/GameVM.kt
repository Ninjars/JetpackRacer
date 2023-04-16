package jez.jetpackracer.features.game

import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jez.jetpackracer.features.game.GameVM.Event
import jez.jetpackracer.utils.toViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GameVM @Inject constructor() : Consumer<Event>, ViewModel(), DefaultLifecycleObserver {

    sealed class Event {
        object Pause : Event()
        object Resume : Event()
        data class GameTimeUpdate(val deltaNanos: Long) : Event()
    }

    sealed class State {
        object Loading : State()
    }

    private val mutableState: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val viewState: StateFlow<GameViewState> =
        mutableState.toViewState(viewModelScope) { state -> StateToViewState(state) }

    override fun accept(event: Event) {
        TODO("Not yet implemented")
    }

    override fun onPause(owner: LifecycleOwner) {
        accept(Event.Pause)
        super.onPause(owner)
    }
}
