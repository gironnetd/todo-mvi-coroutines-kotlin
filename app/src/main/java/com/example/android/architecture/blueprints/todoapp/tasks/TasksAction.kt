package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.mvibase.MviAction

sealed class TasksAction : MviAction {
  data class LoadTasksAction(
      val forceUpdate: Boolean,
      val filterType: TasksFilterType?
  ) : TasksAction()

  data class ActivateTaskAction(val task: Task) : TasksAction()

  data class CompleteTaskAction(val task: Task) : TasksAction()

  object ClearCompletedTasksAction : TasksAction()
}
