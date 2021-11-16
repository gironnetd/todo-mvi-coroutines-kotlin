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

package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.MainCoroutineRule
import com.example.android.architecture.blueprints.todoapp.util.TestObserver
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.test
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [TasksViewModel]
 */
@InternalCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
class TasksViewModelTest {
  @MockK
  private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var tasksViewModel: TasksViewModel
  private lateinit var testObserver: TestObserver<TasksViewState>
  private lateinit var tasks: List<Task>

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @Before
  fun setupTasksViewModel() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockKAnnotations.init(this)

    // Make the sure that all schedulers are immediate.
    schedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    tasksViewModel = TasksViewModel(TasksActionProcessorHolder(tasksRepository, schedulerProvider))

    // We subscribe the tasks to 3, with one active and two completed
    tasks = listOf(
        Task(title = "Title1", description = "Description1", completed = false),
        Task(title = "Title2", description = "Description2", completed = true),
        Task(title = "Title3", description = "Description3", completed = true)
    )
  }

  @Test
  fun loadAllTasksFromRepositoryAndLoadIntoView() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given an initialized TasksViewModel with initialized tasks
    every { tasksRepository.getTasks(any()) } returns flowOf(Result.Success(tasks))
    // When loading of Tasks is initiated
    tasksViewModel.processIntents(flowOf(TasksIntent.InitialIntent))

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }.dispose()
  }

  @Test
  fun loadActiveTasksFromRepositoryAndLoadIntoView() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given an initialized TasksViewModel with initialized tasks
    every { tasksRepository.getTasks(any()) } returns flowOf(Result.Success(tasks))
    // When loading of Tasks is initiated
    tasksViewModel.processIntents(
        flowOf(TasksIntent.ChangeFilterIntent(TasksFilterType.ACTIVE_TASKS))
    )

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }.dispose()
  }

  @Test
  fun loadCompletedTasksFromRepositoryAndLoadIntoView() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given an initialized TasksViewModel with initialized tasks
    every { tasksRepository.getTasks(any()) } returns flowOf(Result.Success(tasks))
    // When loading of Tasks is requested
    tasksViewModel.processIntents(
        flowOf(TasksIntent.ChangeFilterIntent(TasksFilterType.COMPLETED_TASKS))
    )

    // Then progress indicator state is emitted
    testObserver.assertValueAt(1, TasksViewState::isLoading)
    // Then progress indicator state is canceled and all tasks are emitted
    testObserver.assertValueAt(2) { tasksViewState -> !tasksViewState.isLoading }.dispose()
  }

  @Test
  fun completeTask_ShowsTaskMarkedComplete() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given a stubbed task
    val task = Task(title = "Details Requested", description = "For this task")
    // And no tasks available in the repository
    coEvery { tasksRepository.completeTask(task) } just Runs
    every { tasksRepository.getTasks() } returns flowOf(Result.Success(emptyList()))

    // When task is marked as complete
    tasksViewModel.processIntents(flowOf(TasksIntent.CompleteTaskIntent(task)))

    // Then repository is called and task marked complete state is emitted
    coVerify { tasksRepository.completeTask(task) }
    verify { tasksRepository.getTasks() }
    testObserver.assertValueAt(1) { it.uiNotification == TasksViewState.UiNotification.TASK_COMPLETE }.dispose()
  }

  @Test
  fun activateTask_ShowsTaskMarkedActive() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given a stubbed completed task
    val task = Task(title = "Details Requested", description = "For this task", completed = true)
    // And no tasks available in the repository
    coEvery { tasksRepository.activateTask(task) } just Runs
    every { tasksRepository.getTasks() } returns flowOf(Result.Success(emptyList()))

    // When task is marked as activated
    tasksViewModel.processIntents(flowOf(TasksIntent.ActivateTaskIntent(task)))

    // Then repository is called and task marked active state is emitted
    coVerify { tasksRepository.activateTask(task) }
    verify { tasksRepository.getTasks() }
    testObserver.assertValueAt(1) { it.uiNotification == TasksViewState.UiNotification.TASK_ACTIVATED }.dispose()
  }

  @Test
  fun errorLoadingTasks_ShowsError() = mainCoroutineRule.runBlockingTest {
    testObserver = tasksViewModel.states().test(this)
    // Given that no tasks are available in the repository
    every { tasksRepository.getTasks(any()) } returns flowOf(Result.Error(Exception()))

    // When tasks are loaded
    tasksViewModel.processIntents(flowOf(TasksIntent.InitialIntent))

    // Then an error containing state is emitted
    testObserver.assertValueAt(2) { state -> state.error != null }.dispose()
  }
}
