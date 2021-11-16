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

import androidx.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.util.SingletonHolderDoubleArg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * The class is open to allow mocking.
 */
@ExperimentalCoroutinesApi
open class TasksRepository private constructor(
  private val tasksRemoteDataSource: TasksDataSource,
  private val tasksLocalDataSource: TasksDataSource
) : TasksDataSource {

  /**
   * This variable has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  var cachedTasks: MutableMap<String, Task>? = null

  /**
   * Marks the cache as invalid, to force an update the next time data is requested. This variable
   * has package local visibility so it can be accessed from tests.
   */
  @VisibleForTesting
  var cacheIsDirty = false

  private fun getAndCacheLocalTasks(): Flow<Result<List<Task>>>  {
    return tasksLocalDataSource.getTasks()
      .onEach { tasks ->
        (tasks as Result.Success).data.onEach { task -> cachedTasks!![task.id] = task }
      }
  }

  private fun getAndSaveRemoteTasks(): Flow<Result<List<Task>>> {
    return tasksRemoteDataSource.getTasks()
      .onEach { tasks ->
        (tasks as Result.Success).data.onEach { task ->
          tasksLocalDataSource.saveTask(task)
          cachedTasks!![task.id] = task
        }
      }.also { cacheIsDirty = false }
  }

  /**
   * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
   * available first.
   */
  override fun getTasks(): Flow<Result<List<Task>>> {
    // Respond immediately with cache if available and not dirty
    if (cachedTasks != null && !cacheIsDirty) {
      return flowOf(Result.Success(cachedTasks!!.values.toList()))
    } else if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    val remoteTasks = getAndSaveRemoteTasks()

    return if (cacheIsDirty) {
      remoteTasks
    } else {
      // Query the local storage if available. If not, query the network.
      val localTasks = getAndCacheLocalTasks()

      return flow {
        try {
          emit(localTasks.onCompletion {
            if (it == null) emitAll(remoteTasks)
          }.filter { tasks -> !(tasks as Result.Success).data.isEmpty() }.first())
        } catch (e: Exception) {
          emit(Result.Error(e))
        }
      }
    }
  }

  override suspend fun saveTask(task: Task) {
    tasksRemoteDataSource.saveTask(task)
    tasksLocalDataSource.saveTask(task)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!![task.id] = task
  }

  override suspend fun completeTask(task: Task) {
    tasksRemoteDataSource.completeTask(task)
    tasksLocalDataSource.completeTask(task)

    val completedTask =
      Task(title = task.title!!, description = task.description, id = task.id, completed = true)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!![task.id] = completedTask
  }

  override suspend fun completeTask(taskId: String) {
    val taskWithId = getTaskWithId(taskId)
    if (taskWithId != null) {
      completeTask(taskWithId)
    }
  }

  override suspend fun activateTask(task: Task) {
    tasksRemoteDataSource.activateTask(task)
    tasksLocalDataSource.activateTask(task)

    val activeTask =
      Task(title = task.title!!, description = task.description, id = task.id, completed = false)

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!![task.id] = activeTask
  }

  override suspend fun activateTask(taskId: String) {
    val taskWithId = getTaskWithId(taskId)
    if (taskWithId != null) {
      activateTask(taskWithId)
    }
  }

  override suspend fun deleteTask(taskId: String) {
    tasksRemoteDataSource.deleteTask(checkNotNull(taskId))
    tasksLocalDataSource.deleteTask(checkNotNull(taskId))

    cachedTasks!!.remove(taskId)
  }

  override suspend fun clearCompletedTasks() {
    tasksRemoteDataSource.clearCompletedTasks()
    tasksLocalDataSource.clearCompletedTasks()

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    val it = cachedTasks!!.entries.iterator()
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.value.completed) {
        it.remove()
      }
    }
  }

  /**
   * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
   * uses the network data source. This is done to simplify the sample.
   */
  override fun getTask(taskId: String): Flow<Result<Task>> {
    val cachedTask = getTaskWithId(taskId)

    // Respond immediately with cache if available
    if (cachedTask != null) {
      return flowOf(Result.Success(cachedTask))
    }

    // LoadAction from server/persisted if needed.

    // Do in memory cache update to keep the app UI up to date
    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }

    // Is the task in the local data source? If not, query the network.
    val localTask = getTaskWithIdFromLocalRepository(taskId)
    val remoteTask = tasksRemoteDataSource.getTask(taskId)
      .onEach { task ->
        if(task is Result.Success) {
          tasksLocalDataSource.saveTask(task.data)
          cachedTasks!![task.data.id] = task.data
        }
      }

    return flow {
      try {
        emit(localTask.onCompletion {
          if (it == null) emitAll(remoteTask)
        }.first())
      } catch (e: Exception) {
        emit(Result.Error(e))
      }
    }
  }

  override suspend fun refreshTasks() {
    cacheIsDirty = true
  }

  override suspend fun deleteAllTasks() {
    tasksRemoteDataSource.deleteAllTasks()
    tasksLocalDataSource.deleteAllTasks()

    if (cachedTasks == null) {
      cachedTasks = LinkedHashMap()
    }
    cachedTasks!!.clear()
  }

  private fun getTaskWithId(id: String): Task? = cachedTasks?.get(id)

  fun getTaskWithIdFromLocalRepository(taskId: String): Flow<Result<Task>> {
    return flow {
      emit(tasksLocalDataSource.getTask(taskId)
        .onEach { task ->
          if(task is Result.Success) {
            cachedTasks!![taskId] = task.data
          }
        }.first())
    }
  }

  companion object : SingletonHolderDoubleArg<TasksRepository, TasksDataSource, TasksDataSource>(
    ::TasksRepository
  )
}
