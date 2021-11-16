package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent

sealed class TasksIntent : MviIntent {
  object InitialIntent : TasksIntent()

  data class RefreshIntent(val forceUpdate: Boolean) : TasksIntent()

  data class ActivateTaskIntent(val task: Task) : TasksIntent()

  data class CompleteTaskIntent(val task: Task) : TasksIntent()

  object ClearCompletedTasksIntent : TasksIntent()

  data class ChangeFilterIntent(val filterType: TasksFilterType) : TasksIntent()
}
