package com.example.android.architecture.blueprints.todoapp.util

import io.reactivex.observers.BaseTestConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@InternalCoroutinesApi
fun <T> Flow<T>.test(scope: CoroutineScope): TestObserver<T> {
    return TestObserver(scope, this)
}
@InternalCoroutinesApi
class TestObserver<T>(
    scope: CoroutineScope,
    var flow: Flow<T>
): BaseTestConsumer<T, TestObserver<T>>() {

    private val job: Job = scope.launch {
        flow.buffer().collect {
            values.add(it)
        }
    }

    override fun dispose() {
        job.cancel()
    }

    override fun isDisposed(): Boolean {
        return job.isCompleted
    }

    override fun assertSubscribed(): TestObserver<T> {
        if (job == null) {
            throw fail("Not subscribed!")
        }
        return this
    }

    override fun assertNotSubscribed(): TestObserver<T> {
        if (job != null) {
            throw fail("Subscribed!")
        } else if (!errors.isEmpty()) {
            throw fail("Not subscribed but errors found")
        }
        return this
    }
}


