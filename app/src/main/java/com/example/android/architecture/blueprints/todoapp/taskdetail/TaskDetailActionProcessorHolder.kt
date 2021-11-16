package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailAction.*
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailResult.*
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
class TaskDetailActionProcessorHolder(
    private val tasksRepository: TasksRepository,
    private val schedulerProvider: BaseSchedulerProvider
) {

    private val populateTaskProcessor: FlowTransformer<PopulateTaskAction, PopulateTaskResult >
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    tasksRepository.getTask(action.taskId)
                        .map { task -> PopulateTaskResult.Success((task as Result.Success).data) as PopulateTaskResult }
                        .catch { e -> emit(PopulateTaskResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(PopulateTaskResult.InFlight) }
                }
            }
        }

    private val completeTaskProcessor: FlowTransformer<CompleteTaskAction, CompleteTaskResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.completeTask(action.taskId))
                        .flatMapConcat { tasksRepository.getTask(action.taskId) }
                        .flatMapConcat { tasks ->
                            when (tasks) {
                                is Result.Success -> {
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                        CompleteTaskResult.Success(tasks.data),
                                        CompleteTaskResult.HideUiNotification
                                    )
                                }
                                is Result.Loading -> {
                                    flowOf(CompleteTaskResult.InFlight)
                                }
                                is Result.Error -> {
                                    flowOf(CompleteTaskResult.Failure(tasks.exception))
                                }
                            }
                        }
                        .catch { e -> emit(CompleteTaskResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(CompleteTaskResult.InFlight) }
                }
            }
        }

    private val activateTaskProcessor: FlowTransformer<ActivateTaskAction, ActivateTaskResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.activateTask(action.taskId))
                        .flatMapConcat { tasksRepository.getTask(action.taskId) }
                        .flatMapConcat { tasks ->
                            when (tasks) {
                                is Result.Success -> {
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                        ActivateTaskResult.Success(tasks.data),
                                        ActivateTaskResult.HideUiNotification
                                    )
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

    private val deleteTaskProcessor: FlowTransformer<DeleteTaskAction, DeleteTaskResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.deleteTask(action.taskId))
                        .flatMapConcat { flowOf(DeleteTaskResult.Success) as Flow<DeleteTaskResult> }
                        .catch { e -> emit(DeleteTaskResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(DeleteTaskResult.InFlight) }
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

    internal val actionProcessor: FlowTransformer<TaskDetailAction, TaskDetailResult>
        get() = { intents ->
            intents.let { shared ->
                merge(
                    merge(
                        // Match PopulateTasks to populateTaskProcessor
                        shared.filterIsInstance<PopulateTaskAction>().let(populateTaskProcessor),
                        // Match CompleteTaskAction to completeTaskProcessor
                        shared.filterIsInstance<CompleteTaskAction>().let(completeTaskProcessor),
                        // Match ActivateTaskAction to activateTaskProcessor
                        shared.filterIsInstance<ActivateTaskAction>().let(activateTaskProcessor),
                        // Match DeleteTaskAction to deleteTaskProcessor
                        shared.filterIsInstance<DeleteTaskAction>().let(deleteTaskProcessor),
                    ),
                    shared.filter { v ->
                        (v !is PopulateTaskAction
                                && v !is CompleteTaskAction
                                && v !is ActivateTaskAction
                                && v !is DeleteTaskAction)
                    }.flatMapConcat { w ->
                        throw IllegalArgumentException("Unknown Action type: " + w)
                    }
                )
            }
        }
}
