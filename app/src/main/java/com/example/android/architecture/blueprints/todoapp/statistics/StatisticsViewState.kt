package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.mvibase.MviViewState

data class StatisticsViewState(
    val isLoading: Boolean,
    val activeCount: Int,
    val completedCount: Int,
    val error: Throwable?
) : MviViewState {
  companion object {
    fun idle(): StatisticsViewState {
      return StatisticsViewState(
          isLoading = false,
          activeCount = 0,
          completedCount = 0,
          error = null
      )
    }
  }
}
