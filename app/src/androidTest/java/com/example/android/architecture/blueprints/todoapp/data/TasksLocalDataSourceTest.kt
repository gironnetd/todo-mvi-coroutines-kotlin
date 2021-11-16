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

package com.example.android.architecture.blueprints.todoapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the [TasksDataSource], which uses the [TasksDbHelper].
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksLocalDataSourceTest {
  private lateinit var schedulerProvider: BaseSchedulerProvider
  private lateinit var localDataSource: TasksLocalDataSource

  @Before
  fun setup() {
    TasksLocalDataSource.clearInstance()
    schedulerProvider = ImmediateSchedulerProvider()

    localDataSource = TasksLocalDataSource
      .getInstance(InstrumentationRegistry.getInstrumentation().targetContext, schedulerProvider)
  }

  @After
  fun cleanUp() = runBlockingTest {
    localDataSource.deleteAllTasks()
  }

  @Test
  fun testPreConditions() {
    assertNotNull(localDataSource)
  }

  @Test
  fun saveTask_retrievesTask() = runBlockingTest {
    // Given a new task
    val newTask = Task(title = TITLE, description = "")

    // When saved into the persistent repository
    localDataSource.saveTask(newTask)

    // Then the task can be retrieved from the persistent repository
    val testObserver = localDataSource.getTask(newTask.id).test(this)
    testObserver.assertValue(Result.Success(newTask)).dispose()
  }

  @Test
  fun completeTask_retrievedTaskIsComplete() = runBlockingTest {
    // Given a new task in the persistent repository
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)

    // When completed in the persistent repository
    localDataSource.completeTask(newTask)

    // Then the task can be retrieved from the persistent repository and is complete
    val testObserver = localDataSource.getTask(newTask.id).test(this)
    testObserver.assertValueCount(1)
    val (_, _, _, completed) = (testObserver.values()[0] as Result.Success).data
    Assert.assertThat(completed, `is`(true))
    testObserver.dispose()
  }

  @Test
  fun activateTask_retrievedTaskIsActive() = runBlockingTest {
    // Given a new completed task in the persistent repository
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)
    localDataSource.completeTask(newTask)

    // When activated in the persistent repository
    localDataSource.activateTask(newTask)

    // Then the task can be retrieved from the persistent repository and is active
    val testObserver = localDataSource.getTask(newTask.id).test(this)
    testObserver.assertValueCount(1)
    val result = testObserver.values()[0]
    result as Result.Success
    Assert.assertThat(result.data.active, `is`(true))
    Assert.assertThat(result.data.completed, `is`(false))
    testObserver.dispose()
  }

  @Test
  fun clearCompletedTask_taskNotRetrievable() = runBlockingTest {
    // Given 2 new completed tasks and 1 active task in the persistent repository
    val newTask1 = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask1)
    localDataSource.completeTask(newTask1)
    val newTask2 = Task(title = TITLE2, description = "")
    localDataSource.saveTask(newTask2)
    localDataSource.completeTask(newTask2)
    val newTask3 = Task(title = TITLE3, description = "")
    localDataSource.saveTask(newTask3)

    // When completed tasks are cleared in the repository
    localDataSource.clearCompletedTasks()

    // Then the completed tasks cannot be retrieved and the active one can
    val testObserver = localDataSource.getTasks().test(this)
    val result = testObserver.values()[0]
    result as Result.Success
    Assert.assertThat(result.data, not(hasItems(newTask1, newTask2)))
    testObserver.dispose()
  }

  @Test
  fun deleteAllTasks_emptyListOfRetrievedTask() = runBlockingTest {
    // Given a new task in the persistent repository and a mocked callback
    val newTask = Task(title = TITLE, description = "")
    localDataSource.saveTask(newTask)

    // When all tasks are deleted
    localDataSource.deleteAllTasks()

    // Then the retrieved tasks is an empty list
    val testObserver = localDataSource.getTasks().test(this)
    val result = testObserver.values()[0]
    result as Result.Success
    Assert.assertThat(result.data.isEmpty(), `is`(true))
    testObserver.dispose()
  }

  @Test
  fun getTasks_retrieveSavedTasks() = runBlockingTest {
    // Given 2 new tasks in the persistent repository
    val newTask1 = Task(title = TITLE, description = "a")
    localDataSource.saveTask(newTask1)
    val newTask2 = Task(title = TITLE, description = "b")
    localDataSource.saveTask(newTask2)

    // Then the tasks can be retrieved from the persistent repository
    val testObserver = localDataSource.getTasks().test(this)
    val result = testObserver.values()[0]
    result as Result.Success
    Assert.assertThat(result.data, hasItems(newTask1, newTask2))
    testObserver.dispose()
  }

  @Test
  fun getTask_whenTaskNotSaved() = runBlockingTest {
    //Given that no task has been saved
    //When querying for a task, null is returned.
    val testObserver = localDataSource.getTask("1").test(this)
    testObserver.assertEmpty().dispose()
  }

  companion object {
    private const val TITLE = "title"
    private const val TITLE2 = "title2"
    private const val TITLE3 = "title3"
  }}
