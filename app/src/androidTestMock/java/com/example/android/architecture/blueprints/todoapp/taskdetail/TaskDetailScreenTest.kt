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

package com.example.android.architecture.blueprints.todoapp.taskdetail

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.rotateOrientation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the tasks screen, the main screen which contains a list of all tasks.
 */
@FlowPreview
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class TaskDetailScreenTest {

  /**
   * [ActivityTestRule] is a JUnit [Rule] to launch your activity under test.
   *
   *
   *
   *
   * Rules are interceptors which are executed for each test method and are important building
   * blocks of Junit tests.
   *
   *
   *
   *
   * Sometimes an [Activity] requires a custom start [Intent] to receive data
   * from the source Activity. ActivityTestRule has a feature which let's you lazily start the
   * Activity under test, so you can control the Intent that is used to start the target Activity.
   */
  @get:Rule
  var taskDetailActivityTestRule = ActivityTestRule(TaskDetailActivity::class.java, true, false)

  private fun loadActiveTask() {
    startActivityWithWithStubbedTask(ACTIVE_TASK)
  }

  private fun loadCompletedTask() {
    startActivityWithWithStubbedTask(COMPLETED_TASK)
  }

  /**
   * Setup your test fixture with a fake task id. The [TaskDetailActivity] is started with
   * a particular task id, which is then loaded from the service API.
   *
   *
   *
   *
   * Note that this test runs hermetically and is fully isolated using a fake implementation of
   * the service API. This is a great way to make your tests more reliable and faster at the same
   * time, since they are isolated from any outside dependencies.
   */
  private fun startActivityWithWithStubbedTask(task: Task) = runBlockingTest {
    // Add a task stub to the fake service api layer.
    TasksRepository.clearInstance()
    TasksLocalDataSource.clearInstance()
    Injection.provideTasksRepository(InstrumentationRegistry.getInstrumentation().targetContext).saveTask(task)

    // Lazily start the Activity from the ActivityTestRule this time to inject the start Intent
    val startIntent = Intent().apply { putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id) }
    taskDetailActivityTestRule.launchActivity(startIntent)
  }

  @Test
  @Throws(Exception::class)
  fun activeTaskDetails_DisplayedInUi() {
    loadActiveTask()

    // Check that the task title and description are displayed
    onView(withId(R.id.task_detail_title)).check(matches(withText(TASK_TITLE)))
    onView(withId(R.id.task_detail_description)).check(matches(withText(TASK_DESCRIPTION)))
    onView(withId(R.id.task_detail_complete)).check(matches(not(isChecked())))
  }

  @Test
  @Throws(Exception::class)
  fun completedTaskDetails_DisplayedInUi() {
    loadCompletedTask()

    // Check that the task title and description are displayed
    onView(withId(R.id.task_detail_title)).check(matches(withText(TASK_TITLE)))
    onView(withId(R.id.task_detail_description)).check(matches(withText(TASK_DESCRIPTION)))
    onView(withId(R.id.task_detail_complete)).check(matches(isChecked()))
  }

  @Test
  fun orientationChange_menuAndTaskPersist() {
    loadActiveTask()

    // Check that the task is shown
    onView(withId(R.id.task_detail_title)).check(matches(withText(TASK_TITLE)))
    onView(withId(R.id.task_detail_description)).check(matches(withText(TASK_DESCRIPTION)))

    // Check delete menu item is displayed and is unique
    onView(withId(R.id.menu_delete)).check(matches(isDisplayed()))

    rotateOrientation(taskDetailActivityTestRule.activity)

    // Check that the task is shown
    onView(withId(R.id.task_detail_title)).check(matches(withText(TASK_TITLE)))
    onView(withId(R.id.task_detail_description)).check(matches(withText(TASK_DESCRIPTION)))

    // Check delete menu item is displayed and is unique
    onView(withId(R.id.menu_delete)).check(matches(isDisplayed()))
  }

  companion object {
    private val TASK_TITLE = "ATSL"
    private val TASK_DESCRIPTION = "Rocks"
    /**
     * [Task] stub that is added to the fake service API layer.
     */
    private val ACTIVE_TASK = Task(
        title = TASK_TITLE,
        description = TASK_DESCRIPTION,
        completed = false)
    /**
     * [Task] stub that is added to the fake service API layer.
     */
    private val COMPLETED_TASK = Task(
        title = TASK_TITLE,
        description = TASK_DESCRIPTION,
        completed = true)
  }
}
