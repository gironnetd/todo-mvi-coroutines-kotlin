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

package com.example.android.architecture.blueprints.todoapp.data.source.remote

import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.*

/**
 * Implementation of the data source that adds a latency simulating network.
 */
object TasksRemoteDataSource : TasksDataSource {

  private const val SERVICE_LATENCY_IN_MILLIS = 5000
  private val tasksServiceData: MutableMap<String, Task>

  init {
    tasksServiceData = LinkedHashMap(2)
    addTask("Build tower in Pisa", "Ground looks good, no foundation work required.")
    addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!")
  }

  private fun addTask(title: String, description: String) {
    val newTask = Task(title = title, description = description)
    tasksServiceData[newTask.id] = newTask
  }

  override fun getTasks(): Flow<Result<List<Task>>> {
    return flow {
      emit(Result.Success(tasksServiceData.values.toList()))
    }.onStart { delay(SERVICE_LATENCY_IN_MILLIS.toLong()) }
  }

  override fun getTask(taskId: String): Flow<Result<Task>> {
    return flow {
      emit(Result.Success(tasksServiceData[taskId]!!))
    }.onStart { delay(SERVICE_LATENCY_IN_MILLIS.toLong()) }
  }

  override suspend fun saveTask(task: Task) {
    tasksServiceData[task.id] = task
  }

  override suspend fun completeTask(task: Task) {
    val completedTask = Task(task.title!!, task.description, task.id, true)
    tasksServiceData[task.id] = completedTask
  }

  override suspend fun completeTask(taskId: String) {
    // Not required for the remote data source because the {@link TasksRepository} handles
    // converting from a {@code taskId} to a {@link task} using its cached data.
  }

  override suspend fun activateTask(task: Task) {
    val activeTask = Task(title = task.title!!, description = task.description!!, id = task.id)
    tasksServiceData[task.id] = activeTask
  }

  override suspend fun activateTask(taskId: String) {
    // Not required for the remote data source because the {@link TasksRepository} handles
    // converting from a {@code taskId} to a {@link task} using its cached data.
  }

  override suspend fun clearCompletedTasks() {
    val it = tasksServiceData.entries.iterator()
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

  override suspend fun deleteAllTasks() {
    tasksServiceData.clear()
  }

  override suspend fun deleteTask(taskId: String) {
    tasksServiceData.remove(taskId)
  }
}
