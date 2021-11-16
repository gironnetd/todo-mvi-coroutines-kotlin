package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.mvibase.MviIntent

sealed class AddEditTaskIntent : MviIntent {
  data class InitialIntent(val taskId: String?) : AddEditTaskIntent()

  data class SaveTask(
      val taskId: String?,
      val title: String,
      val description: String
  ) : AddEditTaskIntent()
}
