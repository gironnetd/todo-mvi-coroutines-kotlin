/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.MainCoroutineRule
import com.example.android.architecture.blueprints.todoapp.util.TestObserver
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [StatisticsViewModel]
 */
@FlowPreview
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class StatisticsViewModelTest {
  @MockK
  private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var statisticsViewModel: StatisticsViewModel
  private lateinit var testObserver: TestObserver<StatisticsViewState>
  private lateinit var tasks: List<Task>

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @Before
  fun setupStatisticsViewModel() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockKAnnotations.init(this)

    // Make the sure that all schedulers are immediate.
    schedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    statisticsViewModel =
      StatisticsViewModel(StatisticsActionProcessorHolder(tasksRepository, schedulerProvider))

    // We subscribe the tasks to 3, with one active and two completed
    tasks = listOf(
      Task(title = "Title1", description = "Description1"),
      Task(title = "Title2", description = "Description2", completed = true),
      Task(title = "Title3", description = "Description3", completed = true))
  }

  @Test
  fun loadEmptyTasksFromRepository_CallViewToDisplay() = mainCoroutineRule.runBlockingTest {
    // Given an initialized StatisticsViewModel with no tasks
    testObserver = statisticsViewModel.states().test(this)

    tasks = emptyList()
    setTasksAvailable(tasks)

    // When loading of tasks is initiated by first initial intent
    statisticsViewModel.processIntents(flowOf(StatisticsIntent.InitialIntent))

    // Then loading state is emitted
    testObserver.assertValueAt(1, StatisticsViewState::isLoading)

    // Callback is captured and invoked with stubbed tasks
    verify { (tasksRepository).getTasks() }

    // Then not loading, data furnished state in emitted to the view
    testObserver.assertValueAt(2) { (isLoading, activeCount, completedCount) ->
      !isLoading && activeCount == 0 && completedCount == 0
    }.dispose()
  }

  @Test
  fun loadNonEmptyTasksFromRepository_CallViewToDisplay() = mainCoroutineRule.runBlockingTest {
    // Given an initialized StatisticsViewModel with 1 active and 2 completed tasks
    testObserver = statisticsViewModel.states().test(this)
    setTasksAvailable(tasks)

    // When loading of tasks is initiated by first initial intent
    statisticsViewModel.processIntents(flowOf(StatisticsIntent.InitialIntent))

    // Then progress indicator is shown
    testObserver.assertValueAt(1, StatisticsViewState::isLoading)

    // Then progress indicator is hidden and correct data is passed on to the view
    testObserver.assertValueAt(2) { (isLoading, activeCount, completedCount) ->
      !isLoading && activeCount == 1 && completedCount == 2
    }.dispose()
  }

  @Test
  fun loadStatisticsWhenTasksAreUnavailable_CallErrorToDisplay() = mainCoroutineRule.runBlockingTest {
    // Given that tasks data isn't available
    testObserver = statisticsViewModel.states().test(this)
    setTasksNotAvailable()

    // When loading of tasks is initiated by first initial intent
    statisticsViewModel.processIntents(flowOf(StatisticsIntent.InitialIntent))

    // Then an error message is shown
    testObserver.assertValueAt(2) { (_, _, _, error) -> error != null }.dispose()
  }

  private fun setTasksAvailable(tasks: List<Task>?) {
    every { tasksRepository.getTasks() } returns flowOf(Result.Success(tasks!!))
  }

  private fun setTasksNotAvailable() {
    // TODO(benoit) RxJava would print the stacktrace. I'd like to hide that
    every { tasksRepository.getTasks() } returns flowOf(Result.Error(Exception(Throwable("not available"))))
  }
}
