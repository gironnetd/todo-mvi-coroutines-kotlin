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

package com.example.android.architecture.blueprints.todoapp.addedittask

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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [AddEditTaskViewModel].
 */
@InternalCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
class AddEditTaskViewModelTest {

    @MockK
    private lateinit var tasksRepository: TasksRepository
    private lateinit var schedulerProvider: BaseSchedulerProvider
    private lateinit var addEditTaskViewModel: AddEditTaskViewModel
    private lateinit var testObserver: TestObserver<AddEditTaskViewState>

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupMocksAndView() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockKAnnotations.init(this)

        schedulerProvider = ImmediateSchedulerProvider()

        addEditTaskViewModel = AddEditTaskViewModel(
            AddEditTaskActionProcessorHolder(tasksRepository, schedulerProvider)
        )
    }

    @Test
    fun saveNewTaskToRepository_showsSuccessMessageUi() = mainCoroutineRule.runBlockingTest {
        testObserver = addEditTaskViewModel.states().test(this)
        coEvery { tasksRepository.saveTask(any()) } just Runs
        // When task saving intent is emitted by the view
        addEditTaskViewModel.processIntents(
            flowOf(
                AddEditTaskIntent.SaveTask(
                    taskId = null,
                    title = "New Task Title",
                    description = "Some Task Description")
            )
        )

        // Then a task is saved in the repository and the view updates
        coVerify { tasksRepository.saveTask(any()) }

        // saved to the model
        testObserver.assertValueAt(1) { (isEmpty, isSaved) -> isSaved && !isEmpty }.dispose()
    }

    @Test
    fun saveTask_emptyTaskShowsErrorUi() = mainCoroutineRule.runBlockingTest {
        testObserver = addEditTaskViewModel.states().test(this)
        // When an empty task's saving intent is emitted by the view
        addEditTaskViewModel.processIntents(
            flowOf(AddEditTaskIntent.SaveTask(
                taskId = null,
                title = "",
                description = ""))
        )

        // Then an empty task state is emitted back to the view
        coVerify { tasksRepository.saveTask(any()) wasNot Called }
        // saved to the model
        testObserver.assertValueAt(1, AddEditTaskViewState::isEmpty).dispose()
    }

    @Test
    fun saveExistingTaskToRepository_showsSuccessMessageUi() = mainCoroutineRule.runBlockingTest {
        testObserver = addEditTaskViewModel.states().test(this)
        coEvery { tasksRepository.saveTask(any()) } just Runs

        // When an existing task saving intent is emitted by the view
        addEditTaskViewModel.processIntents(
            flowOf(
                AddEditTaskIntent.SaveTask(
                    taskId = "1",
                    title = "Existing Task Title",
                    description = "Some Task Description")
            )
        )

        // Then a task is saved in the repository and the view updates
        coVerify { tasksRepository.saveTask(any()) }
        // saved to the model
        testObserver.assertValueAt(1) { (isEmpty, isSaved) -> isSaved && !isEmpty }.dispose()
    }

    @Test
    fun populateTask_callsRepoAndUpdatesViewOnSuccess() = mainCoroutineRule.runBlockingTest {
        testObserver = addEditTaskViewModel.states().test(this)
        val testTask = Task(title = "TITLE", description = "DESCRIPTION")
        every { tasksRepository.getTask(testTask.id) } returns flowOf(Result.Success(testTask))

        // When populating a task is initiated by an initial intent
        addEditTaskViewModel.processIntents(
            flowOf(
                AddEditTaskIntent.InitialIntent(testTask.id)
            )
        )

        // Then the task repository is queried and a stated is emitted back
        coVerify { tasksRepository.getTask(eq(testTask.id)) }
        testObserver.assertValueAt(1) { (_, _, title, description) ->
          title == testTask.title && description == testTask.description
        }.dispose()
    }

    @Test
    fun populateTask_callsRepoAndUpdatesViewOnError() = mainCoroutineRule.runBlockingTest {
        testObserver = addEditTaskViewModel.states().test(this)
        val (id) = Task(title = "TITLE", description = "DESCRIPTION")
        every { tasksRepository.getTask(id) } returns emptyFlow()

        // When populating a task is initiated by an initial intent
        addEditTaskViewModel.processIntents(
            flowOf(AddEditTaskIntent.InitialIntent(id)
            )
        )

        // Then the task repository is queried and a stated is emitted back
        coVerify { tasksRepository.getTask(eq(id)) }
        testObserver.assertValueAt(1) { (_, _, title, description, error) ->
          error != null && title.isEmpty() && description.isEmpty()
        }.dispose()
    }
}
