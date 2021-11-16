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

import androidx.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.*

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
object FakeTasksRemoteDataSource : TasksDataSource {
  private val TasksServiceData = LinkedHashMap<String, Task>()

  override fun getTasks(): Flow<Result<List<Task>>> {
    return flowOf(Result.Success(TasksServiceData.values.toList()))
  }

  override fun getTask(taskId: String): Flow<Result<Task>> {
    return flowOf(Result.Success(TasksServiceData[taskId]!!))
  }

  override suspend fun saveTask(task: Task) {
    TasksServiceData[task.id] = task
  }

  override suspend fun completeTask(task: Task) {
    val completedTask = Task(task.title!!, task.description, task.id, true)
    TasksServiceData[task.id] = completedTask
  }

  override suspend fun completeTask(taskId: String) {
    val task = TasksServiceData[taskId]!!
    val completedTask = Task(task.title!!, task.description, task.id, true)
    TasksServiceData[taskId] = completedTask
  }

  override suspend fun activateTask(task: Task) {
    val activeTask = Task(title = task.title!!, description = task.description!!, id = task.id)
    TasksServiceData[task.id] = activeTask
  }

  override suspend fun activateTask(taskId: String) {
    val task = TasksServiceData[taskId]!!
    val activeTask = Task(title = task.title!!, description = task.description!!, id = task.id)
    TasksServiceData[taskId] = activeTask
  }

  override suspend fun clearCompletedTasks() {
    val it = TasksServiceData.entries.iterator()
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.value.completed) {
        it.remove()
      }
    }
  }

  override suspend fun refreshTasks() {
    // Not required because the {@link TasksRepository} handles the logic of refreshing the
    // tasks from all the available data sources.
  }

  override suspend fun deleteTask(taskId: String) {
    TasksServiceData.remove(taskId)
  }

  override suspend fun deleteAllTasks() {
    TasksServiceData.clear()
  }

  @VisibleForTesting
  fun addTasks(vararg tasks: Task) {
    for (task in tasks) {
      TasksServiceData[task.id] = task
    }
  }
}
