package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult

sealed class TaskDetailResult : MviResult {

  sealed class PopulateTaskResult : TaskDetailResult() {
    data class Success(val task: Task) : PopulateTaskResult()
    data class Failure(val error: Throwable) : PopulateTaskResult()
    object InFlight : PopulateTaskResult()
  }

  sealed class ActivateTaskResult : TaskDetailResult() {
    data class Success(val task: Task) : ActivateTaskResult()
    data class Failure(val error: Throwable) : ActivateTaskResult()
    object InFlight : ActivateTaskResult()
    object HideUiNotification : ActivateTaskResult()
  }

  sealed class CompleteTaskResult : TaskDetailResult() {
    data class Success(val task: Task) : CompleteTaskResult()
    data class Failure(val error: Throwable) : CompleteTaskResult()
    object InFlight : CompleteTaskResult()
    object HideUiNotification : CompleteTaskResult()
  }

  sealed class DeleteTaskResult : TaskDetailResult() {
    object Success : DeleteTaskResult()
    data class Failure(val error: Throwable) : DeleteTaskResult()
    object InFlight : DeleteTaskResult()
  }
}
