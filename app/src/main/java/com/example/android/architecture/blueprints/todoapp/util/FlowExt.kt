package com.example.android.architecture.blueprints.todoapp.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

/**
 * Emit an event immediately, then emit an other event after a delay has passed.
 * It is used for time limited UI state (e.g. a snackbar) to allow the stream to control
 * the timing for the showing and the hiding of a UI component.
 *
 * @param immediate Immediately emitted event
 * @param delayed   Event emitted after a delay
 */
fun <T> pairWithDelay(immediate: T, delayed: T): Flow<T> {
    return flowOf(delayed)
        .onStart {
            emit(immediate)
            delay(2000)
        }
}

typealias FlowTransformer<I, O> = (Flow<I>) -> Flow<O>


