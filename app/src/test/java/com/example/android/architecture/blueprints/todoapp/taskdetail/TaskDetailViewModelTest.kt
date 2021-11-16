package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailIntent.*
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailViewState.UiNotification.*
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
import java.util.*

/**
 * Unit tests for the implementation of [TaskDetailViewModel]
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@FlowPreview
class TaskDetailViewModelTest {

  private lateinit var taskDetailViewModel: TaskDetailViewModel
  @MockK private lateinit var tasksRepository: TasksRepository
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var testObserver: TestObserver<TaskDetailViewState>

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @Before
  @Throws(Exception::class)
  fun setUpTaskDetailViewModel() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockKAnnotations.init(this)

    // Make the sure that all schedulers are immediate.
    schedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    taskDetailViewModel = TaskDetailViewModel(
        TaskDetailActionProcessorHolder(tasksRepository, schedulerProvider)
    )
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnSuccess() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)

    val testTask = Task(title = "TITLE", description = "DESCRIPTION")
    every { tasksRepository.getTask(testTask.id) } returns flowOf(Result.Success(testTask))

    // When populating a task is initiated by an initial intent
    taskDetailViewModel.processIntents(flowOf(InitialIntent(testTask.id)))

    // Then the task repository is queried and a stated is emitted back
    verify { tasksRepository.getTask(eq(testTask.id)) }
    testObserver.assertValueAt(2) { (title, description) ->
      title == testTask.title && description == testTask.description
    }.dispose()
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnError() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)
    val (id) = Task(title = "TITLE", description = "DESCRIPTION")
    every { tasksRepository.getTask(id) } returns flowOf(Result.Error(NoSuchElementException("The MaybeSource is empty")))

    // When populating a task is initiated by an initial intent
    taskDetailViewModel.processIntents(flowOf(InitialIntent(id)))

    // Then the task repository is queried and a stated is emitted back
    verify { tasksRepository.getTask(eq(id)) }
    testObserver.assertValueAt(2) { (title, description, _, _, error) ->
      error != null && title.isEmpty() && description.isEmpty()
    }.dispose()
  }

  @Test
  fun deleteTask_deletesFromRepository_showsSuccessMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)

    coEvery { tasksRepository.deleteTask(any()) } just Runs

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(DeleteTask("1")))

    // Then a task is saved in the repository and the view updates
    coVerify { tasksRepository.deleteTask(any()) }
    // saved to the model
    testObserver.assertValueAt(1) { it.uiNotification == TASK_DELETED }.dispose()
  }

  @Test
  fun deleteTask_showsErrorMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)
    coEvery { tasksRepository.deleteTask(any()) } just Runs

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(DeleteTask("1")))

    // Then a task is saved in the repository and the view updates
    coVerify { tasksRepository.deleteTask(any()) }

    // saved to the model
    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }.dispose()
  }

  @Test
  fun completeTask_marksTaskAsComplete_showsSuccessMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)
    val task = Task(
        title = "Complete Requested",
        description = "For this task"
    )
    coEvery { tasksRepository.completeTask(any<String>()) } just Runs
    every { tasksRepository.getTask(any()) } returns flowOf(Result.Success(task))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(CompleteTaskIntent("1")))

    // Then a task is saved in the repository and the view updates
    coVerify { tasksRepository.completeTask(any<String>()) }
    verify { tasksRepository.getTask(any()) }
    testObserver.assertValueAt(1) { it.uiNotification == TASK_COMPLETE }.dispose()
  }

  @Test
  fun completeTask_showsErrorMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)
    coEvery { tasksRepository.completeTask(any<String>()) } just Runs
    every { tasksRepository.getTask(any()) } returns flowOf(Result.Error(NoSuchElementException("The MaybeSource is empty")))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(CompleteTaskIntent("1")))

    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }.dispose()
  }

  @Test
  fun activateTask_marksTaskAsActive_showsSuccessMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)
    val task = Task(
        title = "Activate Requested",
        description = "For this task"
    )
    coEvery { tasksRepository.activateTask(any<String>()) } just Runs
    every { tasksRepository.getTask(any()) } returns flowOf(Result.Success(task))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(ActivateTaskIntent("1")))

    // Then a task is saved in the repository and the view updates
    coVerify { tasksRepository.activateTask(any<String>()) }
    verify { tasksRepository.getTask(any()) }
    testObserver.assertValueAt(1) { it.uiNotification == TASK_ACTIVATED }.dispose()
  }

  @Test
  fun activateTask_showsErrorMessageUi() = mainCoroutineRule.runBlockingTest {
    testObserver = taskDetailViewModel.states().test(this)

    coEvery { tasksRepository.activateTask(any<String>()) } just Runs
    every { tasksRepository.getTask(any()) } returns flowOf(Result.Error(NoSuchElementException("The MaybeSource is empty")))

    // When an existing task saving intent is emitted by the view
    taskDetailViewModel.processIntents(flowOf(ActivateTaskIntent("1")))

    testObserver.assertValueAt(1) { (_, _, _, _, error) -> error != null }.also {
      it.dispose()
    }
  }
}