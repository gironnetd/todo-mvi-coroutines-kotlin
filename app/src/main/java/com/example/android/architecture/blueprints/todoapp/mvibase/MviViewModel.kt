package com.example.android.architecture.blueprints.todoapp.mvibase

import kotlinx.coroutines.flow.Flow

/**
 * Object that will subscribes to a [MviView]'s [MviIntent]s,
 * process it and emit a [MviViewState] back.
 *
 * @param I Top class of the [MviIntent] that the [MviViewModel] will be subscribing
 * to.
 * @param S Top class of the [MviViewState] the [MviViewModel] will be emitting.
 */
interface MviViewModel<I : MviIntent, S : MviViewState> {

  fun processIntents(intents: Flow<I>)

  fun states(): Flow<S>
}
