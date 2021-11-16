package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskAction.*
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskResult.*
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.util.FlowTransformer
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
class AddEditTaskActionProcessorHolder(
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

    private val createTaskProcessor: FlowTransformer<CreateTaskAction, CreateTaskResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(Task(title = action.title, description = action.description))
                        .flatMapConcat { task ->
                            when(task.empty) {
                                true -> flowOf(CreateTaskResult.Empty)
                                false -> {
                                    flowOf(tasksRepository.saveTask(task))
                                        .flatMapConcat {
                                            flowOf(CreateTaskResult.Success )
                                        }
                                }
                            }
                        }

                }
            }
        }

    private val updateTaskProcessor: FlowTransformer<UpdateTaskAction, UpdateTaskResult>
        get() = { actions ->
            actions.flatMapConcat { action ->
                withContext(Dispatchers.Main) {
                    flowOf(tasksRepository.saveTask(
                        Task(title = action.title, description = action.description, id = action.taskId)
                    )).flatMapConcat {
                        flowOf(UpdateTaskResult)
                    }
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
    internal val actionProcessor: FlowTransformer<AddEditTaskAction, AddEditTaskResult >
        get() =  { intents ->
            intents.let { shared ->
                merge(
                    merge(
                        // Match PopulateTasks to populateTaskProcessor
                        shared.filterIsInstance<PopulateTaskAction>().let(populateTaskProcessor),
                        // Match CreateTasks to createTaskProcessor
                        shared.filterIsInstance<CreateTaskAction>().let(createTaskProcessor),
                        // Match UpdateTasks to updateTaskProcessor
                        shared.filterIsInstance<UpdateTaskAction>().let(updateTaskProcessor)
                    ),
                    shared.filter { v ->
                        v !is PopulateTaskAction &&
                                v !is CreateTaskAction &&
                                v !is UpdateTaskAction
                    }.flatMapConcat { w ->
                        throw  IllegalArgumentException("Unknown Action type: $w")
                    }
                )
            }
        }
}
