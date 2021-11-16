package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState

data class TaskDetailViewState(
  val title: String,
  val description: String,
  val active: Boolean,
  val loading: Boolean,
  val error: Throwable?,
  val uiNotification: UiNotification?
) : MviViewState {
  enum class UiNotification {
    TASK_COMPLETE,
    TASK_ACTIVATED,
    TASK_DELETED
  }

  companion object {
    fun idle(): TaskDetailViewState {
      return TaskDetailViewState(
          title = "",
          description = "",
          active = false,
          loading = false,
          error = null,
          uiNotification = null
      )
    }
  }
}
