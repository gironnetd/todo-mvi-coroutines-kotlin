package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.tasks.TasksAction.*
import com.example.android.architecture.blueprints.todoapp.tasks.TasksResult.*
import com.example.android.architecture.blueprints.todoapp.util.FlowTransformer
import com.example.android.architecture.blueprints.todoapp.util.pairWithDelay
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Contains and executes the business logic for all emitted [MviAction]
 * and returns one unique [Flow] of [MviResult].
 *
 *
 * This could have been included inside the [MviViewModel]
 * but was separated to ease maintenance, as the [MviViewModel] was getting too big.
 */

@FlowPreview
@ExperimentalCoroutinesApi
class TasksActionProcessorHolder(
    private val tasksRepository: TasksRepository,
    private val schedulerProvider: BaseSchedulerProvider
) {

    private val loadTasksProcessor: FlowTransformer<LoadTasksAction, LoadTasksResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    tasksRepository.getTasks(action.forceUpdate)
                        .map { tasks -> LoadTasksResult.Success((tasks as Result.Success).data, action.filterType) as LoadTasksResult }
                        .catch { e -> emit(LoadTasksResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(LoadTasksResult.InFlight) }
                }
            }
        }

    private val activateTaskProcessor: FlowTransformer<ActivateTaskAction, ActivateTaskResult>
        get()  = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.activateTask(action.task))
                        .flatMapConcat { tasksRepository.getTasks() }
                        .flatMapConcat { tasks ->
                            when (tasks) {
                                is Result.Success -> {
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                        ActivateTaskResult.Success(tasks.data),
                                        ActivateTaskResult.HideUiNotification)
                                }
                                is Result.Loading -> {
                                    flowOf(ActivateTaskResult.InFlight)
                                }
                                is Result.Error -> {
                                    flowOf(ActivateTaskResult.Failure(tasks.exception))
                                }
                            }
                        }
                        .catch { e -> emit(ActivateTaskResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(ActivateTaskResult.InFlight) }
                }
            }
        }

    private val completeTaskProcessor: FlowTransformer<CompleteTaskAction, CompleteTaskResult>
        get()  = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.completeTask(action.task))
                        .flatMapConcat { tasksRepository.getTasks() }
                        .flatMapConcat { tasks ->
                            when (tasks) {
                                is Result.Success -> {
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                        CompleteTaskResult.Success(tasks.data),
                                        CompleteTaskResult.HideUiNotification)
                                }
                                is Result.Loading -> {
                                    flowOf(CompleteTaskResult.InFlight)
                                }
                                is Result.Error -> {
                                   flowOf(CompleteTaskResult.Failure(tasks.exception))
                                }
                                else -> {
                                    flowOf()
                                }
                            }
                        }
                        .catch { e -> emit(CompleteTaskResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(CompleteTaskResult.InFlight) }
                }
            }
        }

    private val clearCompletedTasksProcessor: FlowTransformer<ClearCompletedTasksAction, ClearCompletedTasksResult>
        get() = { actions ->
            actions.flatMapConcat {
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.clearCompletedTasks())
                        .flatMapConcat { tasksRepository.getTasks() }
                        .flatMapConcat { tasks ->
                            when (tasks) {
                                is Result.Success -> {
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                        ClearCompletedTasksResult.Success(tasks.data),
                                        ClearCompletedTasksResult.HideUiNotification)
                                }
                                is Result.Loading -> {
                                    flowOf(ClearCompletedTasksResult.InFlight)
                                }
                                is Result.Error -> {
                                    flowOf(ClearCompletedTasksResult.Failure(tasks.exception))
                                }
                                else -> {
                                    flowOf()
                                }
                            }
                        }
                        .catch { e -> emit(ClearCompletedTasksResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(ClearCompletedTasksResult.InFlight) }
                }
            }
        }

    /**
     * Splits the [Flow] to match each type of [MviAction] to
     * its corresponding business logic processor. Each processor takes a defined [MviAction],
     * returns a defined [MviResult]
     * The global actionProcessor then merges all [Flow] back to
     * one unique [Flow].
     *
     *
     * The splitting is done using [Flow.let] which allows almost anything
     * on the passed [Flow] as long as one and only one [Flow] is returned.
     *
     *
     * An security layer is also added for unhandled [MviAction] to allow early crash
     * at runtime to easy the maintenance.
     */
    internal val actionProcessor: FlowTransformer<TasksAction, TasksResult>
        get() =  { intents ->
            intents.let { shared ->
                    merge(
                        merge(
                            // Match LoadTasksAction to loadTasksProcessor
                            shared.filterIsInstance<LoadTasksAction>().let(loadTasksProcessor),
                            // Match ActivateTaskAction to populateTaskProcessor
                            shared.filterIsInstance<ActivateTaskAction>().let(activateTaskProcessor),
                            // Match CompleteTaskAction to completeTaskProcessor
                            shared.filterIsInstance<CompleteTaskAction>().let(completeTaskProcessor),
                            // Match ClearCompletedTasksAction to clearCompletedTasksProcessor
                            shared.filterIsInstance<ClearCompletedTasksAction>().let(clearCompletedTasksProcessor)
                        ),
                        shared.filter { v ->
                            v !is LoadTasksAction
                                    && v !is ActivateTaskAction
                                    && v !is CompleteTaskAction
                                    && v !is ClearCompletedTasksAction
                        }.flatMapConcat { w ->
                            throw  IllegalArgumentException("Unknown Action type: $w")
                        }
                    )
                }
        }
}
