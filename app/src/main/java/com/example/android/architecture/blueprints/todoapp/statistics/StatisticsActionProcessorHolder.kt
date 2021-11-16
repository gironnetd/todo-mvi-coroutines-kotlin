package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsAction.LoadStatisticsAction
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsResult.LoadStatisticsResult
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
class StatisticsActionProcessorHolder(
    private val tasksRepository: TasksRepository,
    private val schedulerProvider: BaseSchedulerProvider
) {

    private val loadStatisticsProcessor: FlowTransformer<LoadStatisticsAction, LoadStatisticsResult>
        get() = { actions ->
            actions.flatMapConcat {
                withContext(Dispatchers.Main) {
                    tasksRepository.getTasks()
                        .map { tasks ->
                            val activeCount = (tasks as Result.Success).data.filter { task -> task.active }.count()
                            val completeCount = (tasks as Result.Success).data.filter { task -> task.completed }.count()

                            LoadStatisticsResult.Success(activeCount, completeCount) as LoadStatisticsResult
                        }
                        .catch { e -> emit(LoadStatisticsResult.Failure(e)) }
                        .flowOn(Dispatchers.IO)
                        .onStart { emit(LoadStatisticsResult.InFlight) }
                }
            }
        }

    /**
     * Splits the [Flow] to match each type of [MviAction] to its corresponding business logic
     * processor. Each processor takes a defined [MviAction], returns a defined [MviResult].
     * The global actionProcessor then merges all [Flow] back to one unique [Flow].
     *
     * The splitting is done using [Flow.let] which allows almost anything
     * on the passed [Flow] as long as one and only one [Flow] is returned.
     *
     * An security layer is also added for unhandled [MviAction] to allow early crash
     * at runtime to easy the maintenance.
     */
    internal val actionProcessor: FlowTransformer<StatisticsAction, StatisticsResult >
        get() =  { intents ->
            intents.let { shared ->
                merge(
                    // Match LoadStatisticsResult to loadStatisticsProcessor
                    shared.filterIsInstance<LoadStatisticsAction>().let(loadStatisticsProcessor),
                    shared.filter { v ->
                        v !is LoadStatisticsAction
                    }.flatMapConcat { w ->
                        throw  IllegalArgumentException("Unknown Action type: $w")
                    }
                )
            }
        }
}
