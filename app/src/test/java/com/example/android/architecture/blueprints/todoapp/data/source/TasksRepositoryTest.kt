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

package com.example.android.architecture.blueprints.todoapp.data.source

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.util.MainCoroutineRule
import com.example.android.architecture.blueprints.todoapp.util.TestObserver
import com.example.android.architecture.blueprints.todoapp.util.test
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class TasksRepositoryTest {
  private lateinit var tasksRepository: TasksRepository
  private lateinit var tasksTestObserver: TestObserver<Result<List<Task>>>
  @MockK(relaxed = true) private lateinit var tasksRemoteDataSource: TasksDataSource
  @MockK(relaxed = true) private lateinit var tasksLocalDataSource: TasksDataSource

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @Before
  fun setupTasksRepository() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockKAnnotations.init(this)

    // Get a reference to the class under test
    tasksRepository = TasksRepository.getInstance(tasksRemoteDataSource, tasksLocalDataSource)
  }

  @After
  fun destroyRepositoryInstance() {
    TasksRepository.clearInstance()
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInLocalStorage() = mainCoroutineRule.runBlockingTest {
    // Given that the local data source has data available
    setTasksAvailable(tasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When two subscriptions are set
    val testObserver1 = tasksRepository.getTasks().test(this)
    val testObserver2 = tasksRepository.getTasks().test(this)

    // Then tasks were only requested once from remote and local sources
    coVerify { (tasksRemoteDataSource).getTasks() }
    coVerify { (tasksLocalDataSource).getTasks() }

    assertFalse(tasksRepository.cacheIsDirty)
    testObserver1.assertValue(Result.Success(TASKS))
    testObserver2.assertValue(Result.Success(TASKS))
  }

  @Test
  fun getTasks_repositoryCachesAfterFirstSubscription_whenTasksAvailableInRemoteStorage() = mainCoroutineRule.runBlockingTest {
    // Given that the local data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksLocalDataSource)

    // When two subscriptions are set
    val testObserver1 = tasksRepository.getTasks().test(this)
    val testObserver2 = tasksRepository.getTasks().test(this)

    // Then tasks were only requested once from remote and local sources
    coVerify { (tasksRemoteDataSource).getTasks() }
    coVerify { (tasksLocalDataSource).getTasks() }
    assertFalse(tasksRepository.cacheIsDirty)
    testObserver1.assertValue(Result.Success(TASKS))
    testObserver2.assertValue(Result.Success(TASKS))
  }

  @Test
  fun getTasks_requestsAllTasksFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
    // Given that the local data source has data available
    setTasksAvailable(tasksLocalDataSource, TASKS)
    // And the remote data source does not have any data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When tasks are requested from the tasks repository
    tasksTestObserver = tasksRepository.getTasks().test(this)

    // Then tasks are loaded from the local data source
    coVerify { (tasksLocalDataSource).getTasks() }
    tasksTestObserver.assertValue(Result.Success(TASKS))
  }

  @Test
  fun saveTask_savesTaskToServiceAPI() = mainCoroutineRule.runBlockingTest {
    // Given a stub task with title and description
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description")

    // When a task is saved to the tasks repository
    tasksRepository.saveTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).saveTask(newTask) }
    coVerify { (tasksLocalDataSource).saveTask(newTask) }
    assertThat(tasksRepository.cachedTasks!!.size, `is`(1))
  }

  @Test
  fun completeTask_completesTaskToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given a stub active task with title and description added in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description")
    tasksRepository.saveTask(newTask)

    // When a task is completed to the tasks repository
    tasksRepository.completeTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).completeTask(newTask) }
    coVerify { (tasksLocalDataSource).completeTask(newTask) }

    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(false))
    }
  }

  @Test
  fun completeTaskId_completesTaskToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given a stub active task with title and description added in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description")
    tasksRepository.saveTask(newTask)

    // When a task is completed using its id to the tasks repository
    tasksRepository.completeTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).completeTask(newTask) }
    coVerify { (tasksLocalDataSource).completeTask(newTask) }
    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(false))
    }
  }

  @Test
  fun activateTask_activatesTaskToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given a stub completed task with title and description in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    tasksRepository.saveTask(newTask)

    // When a completed task is activated to the tasks repository
    tasksRepository.activateTask(newTask)

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).activateTask(newTask) }
    coVerify { (tasksLocalDataSource).activateTask(newTask) }

    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(true))
    }
  }

  @Test
  fun activateTaskId_activatesTaskToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given a stub completed task with title and description in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    tasksRepository.saveTask(newTask)

    // When a completed task is activated with its id to the tasks repository
    tasksRepository.activateTask(newTask.id)

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).activateTask(newTask) }
    coVerify { (tasksLocalDataSource).activateTask(newTask) }
    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertThat(cachedTasks[newTask.id]!!.active, `is`(true))
    }
  }

  @Test
  fun getTask_requestsSingleTaskFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
    // Given a stub completed task with title and description in the local repository
    val task = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    setTaskAvailable(tasksLocalDataSource, task)
    // And the task not available in the remote repository
    val exception = Result.Error(NoSuchElementException("The MaybeSource is empty"))
    setTaskNotAvailable(tasksRemoteDataSource, task.id, exception)

    // When a task is requested from the tasks repository
    val testObserver = tasksRepository.getTask(task.id).test(this)

    // Then the task is loaded from the database
    coVerify { (tasksLocalDataSource).getTask(task.id) }
    testObserver.assertValue(Result.Success(task))
  }

  @Test
  fun getTask_whenDataNotLocal_fails() = mainCoroutineRule.runBlockingTest {
    // Given a stub completed task with title and description in the remote repository
    val task = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    setTaskAvailable(tasksRemoteDataSource, task)
    // And the task not available in the local repository
    val exception = Result.Error(NoSuchElementException("The MaybeSource is empty"))
    setTaskNotAvailable(tasksLocalDataSource, task.id, exception)

    // When a task is requested from the tasks repository
    val testObserver = tasksRepository.getTask(task.id).test(this)

    // Verify no data is returned
    testObserver.assertValue(exception)
  }

  @Test
  fun deleteCompletedTasks_deleteCompletedTasksToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    tasksRepository.saveTask(newTask)
    val newTask2 = Task(title = TASK_TITLE2, description = "Some Task Description")
    tasksRepository.saveTask(newTask2)
    val newTask3 = Task(title = TASK_TITLE3, description = "Some Task Description",
      completed = true)
    tasksRepository.saveTask(newTask3)

    // When a completed tasks are cleared to the tasks repository
    tasksRepository.clearCompletedTasks()

    // Then the service API and persistent repository are called and the cache is updated
    coVerify { (tasksRemoteDataSource).clearCompletedTasks() }
    coVerify { (tasksLocalDataSource).clearCompletedTasks() }

    tasksRepository.cachedTasks!!.let { cachedTasks ->
      assertThat(cachedTasks.size, `is`(1))
      assertTrue(cachedTasks[newTask2.id]!!.active)
      assertThat<String>(cachedTasks[newTask2.id]!!.title, `is`(TASK_TITLE2))
    }
  }

  @Test
  fun deleteAllTasks_deleteTasksToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
    // Given 2 stub completed tasks and 1 stub active tasks in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    tasksRepository.saveTask(newTask)
    val newTask2 = Task(title = TASK_TITLE2, description = "Some Task Description")
    tasksRepository.saveTask(newTask2)
    val newTask3 = Task(title = TASK_TITLE3, description = "Some Task Description",
      completed = true)
    tasksRepository.saveTask(newTask3)

    // When all tasks are deleted to the tasks repository
    tasksRepository.deleteAllTasks()

    // Verify the data sources were called
    coVerify { (tasksRemoteDataSource).deleteAllTasks() }
    coVerify { (tasksLocalDataSource).deleteAllTasks() }

    assertThat(tasksRepository.cachedTasks!!.size, `is`(0))
  }

  @Test
  fun deleteTask_deleteTaskToServiceAPIRemovedFromCache() = mainCoroutineRule.runBlockingTest {
    // Given a task in the repository
    val newTask = Task(title = TASK_TITLE, description = "Some Task Description", completed = true)
    tasksRepository.saveTask(newTask)
    assertThat(tasksRepository.cachedTasks!!.containsKey(newTask.id), `is`(true))

    // When deleted
    tasksRepository.deleteTask(newTask.id)

    // Verify the data sources were called
    coVerify { (tasksRemoteDataSource).deleteTask(newTask.id) }
    coVerify { (tasksLocalDataSource).deleteTask(newTask.id) }

    // Verify it's removed from repository
    assertThat(tasksRepository.cachedTasks!!.containsKey(newTask.id), `is`(false))
  }

  @Test
  fun getTasksWithDirtyCache_tasksAreRetrievedFromRemote() = mainCoroutineRule.runBlockingTest {
    // Given that the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository with dirty cache
    tasksRepository.refreshTasks()
    tasksTestObserver = tasksRepository.getTasks().test(this)

    // Verify the tasks from the remote data source are returned, not the local
    coVerify { (tasksRemoteDataSource).getTasks() wasNot Called }
    coVerify { (tasksLocalDataSource).getTasks() }
    tasksTestObserver.assertValue(Result.Success(TASKS))
  }

  @Test
  fun getTasksWithLocalDataSourceUnavailable_tasksAreRetrievedFromRemote() = mainCoroutineRule.runBlockingTest {
    // Given that the local data source has no data available
    setTasksNotAvailable(tasksLocalDataSource)
    // And the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // When calling getTasks in the repository
    tasksTestObserver = tasksRepository.getTasks().test(this)

    // Verify the tasks from the remote data source are returned
    coVerify { (tasksRemoteDataSource).getTasks() }
    tasksTestObserver.assertValue(Result.Success(TASKS))
  }

  @Test
  fun getTasksWithBothDataSourcesUnavailable_firesOnDataUnavailable() = mainCoroutineRule.runBlockingTest {
    // Given that the local data source has no data available
    setTasksNotAvailable(tasksLocalDataSource)
    // And the remote data source has no data available
    setTasksNotAvailable(tasksRemoteDataSource)

    // When calling getTasks in the repository
    tasksTestObserver = tasksRepository.getTasks().test(this)

    // Verify no data is returned
    tasksTestObserver.assertValueCount(1)
    // Verify that error is returned
    tasksTestObserver.assertValue { it is Result.Error }
  }

  @Test
  fun getTaskWithBothDataSourcesUnavailable_firesOnError() = mainCoroutineRule.runBlockingTest {
    // Given a task id
    val taskId = "123"
    val exception = Result.Error(NoSuchElementException("The MaybeSource is empty"))
    // And the local data source has no data available
    setTaskNotAvailable(tasksLocalDataSource, taskId, exception)
    // And the remote data source has no data available
    setTaskNotAvailable(tasksRemoteDataSource, taskId, exception)

    // When calling getTask in the repository
    val testObserver = tasksRepository.getTask(taskId).test(this)

    // Verify that error is returned
    testObserver.assertValue(exception)
  }

  @Test
  fun getTasks_refreshesLocalDataSource() = mainCoroutineRule.runBlockingTest {
    // Given that the remote data source has data available
    setTasksAvailable(tasksRemoteDataSource, TASKS)

    // Mark cache as dirty to force a reload of data from remote data source.
    tasksRepository.refreshTasks()

    // When calling getTasks in the repository
    tasksTestObserver = tasksRepository.getTasks().test(this)

    // Verify that the data fetched from the remote data source was saved in local.
    coVerify(exactly = TASKS.size) { (tasksLocalDataSource).saveTask(any()) }
    tasksTestObserver.assertValue(Result.Success(TASKS))
  }

  private fun setTasksNotAvailable(dataSource: TasksDataSource)  {
    every { dataSource.getTasks() } returns flowOf(Result.Success(emptyList()))
  }

  private fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>)  {
    every { dataSource.getTasks() } returns flowOf(Result.Success(tasks)).onCompletion { if (it == null) emitAll(flowOf()) }
  }

  private fun setTaskNotAvailable(dataSource: TasksDataSource, taskId: String, exception: Result.Error) {
    every { dataSource.getTask(taskId) } returns flowOf(exception)
  }

  private fun setTaskAvailable(dataSource: TasksDataSource, task: Task)  {
    every { dataSource.getTask(task.id) } returns flowOf(Result.Success(task))
  }

  companion object {
    private const val TASK_TITLE = "title"
    private const val TASK_TITLE2 = "title2"
    private const val TASK_TITLE3 = "title3"
    private val TASKS = listOf(
      Task(title = "Title1", description = "Description1"),
      Task(title = "Title2", description = "Description2"))
  }
}
