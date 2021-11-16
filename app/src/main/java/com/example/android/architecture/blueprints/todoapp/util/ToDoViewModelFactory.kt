package com.example.android.architecture.blueprints.todoapp.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActionProcessorHolder
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskViewModel
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsActionProcessorHolder
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsViewModel
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailActionProcessorHolder
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailViewModel
import com.example.android.architecture.blueprints.todoapp.tasks.TasksActionProcessorHolder
import com.example.android.architecture.blueprints.todoapp.tasks.TasksViewModel

class ToDoViewModelFactory private constructor(
    private val applicationContext: Context
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass == StatisticsViewModel::class.java) {
      return StatisticsViewModel(
          StatisticsActionProcessorHolder(
              Injection.provideTasksRepository(applicationContext),
              Injection.provideSchedulerProvider())) as T
    }
    if (modelClass == TasksViewModel::class.java) {
      return TasksViewModel(
          TasksActionProcessorHolder(
              Injection.provideTasksRepository(applicationContext),
              Injection.provideSchedulerProvider())) as T
    }
    if (modelClass == AddEditTaskViewModel::class.java) {
      return AddEditTaskViewModel(
          AddEditTaskActionProcessorHolder(
              Injection.provideTasksRepository(applicationContext),
              Injection.provideSchedulerProvider())) as T
    }
    if (modelClass == TaskDetailViewModel::class.java) {
      return TaskDetailViewModel(
          TaskDetailActionProcessorHolder(
              Injection.provideTasksRepository(applicationContext),
              Injection.provideSchedulerProvider())) as T
    }
    throw IllegalArgumentException("unknown model class " + modelClass)
  }

  companion object : SingletonHolderSingleArg<ToDoViewModelFactory, Context>(::ToDoViewModelFactory)
}
