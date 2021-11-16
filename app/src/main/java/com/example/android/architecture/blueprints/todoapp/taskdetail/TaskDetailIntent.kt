package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent

sealed class TaskDetailIntent : MviIntent {
  data class InitialIntent(val taskId: String) : TaskDetailIntent()
  data class DeleteTask(val taskId: String) : TaskDetailIntent()
  data class ActivateTaskIntent(val taskId: String) : TaskDetailIntent()
  data class CompleteTaskIntent(val taskId: String) : TaskDetailIntent()
}
