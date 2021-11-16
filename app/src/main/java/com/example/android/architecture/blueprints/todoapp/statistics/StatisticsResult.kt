package com.example.android.architecture.blueprints.todoapp.statistics

import com.example.android.architecture.blueprints.todoapp.mvibase.MviResult

sealed class StatisticsResult : MviResult {
  sealed class LoadStatisticsResult : StatisticsResult() {
    data class Success(val activeCount: Int, val completedCount: Int) : LoadStatisticsResult()
    data class Failure(val error: Throwable) : LoadStatisticsResult()
    object InFlight : LoadStatisticsResult()
  }
}
