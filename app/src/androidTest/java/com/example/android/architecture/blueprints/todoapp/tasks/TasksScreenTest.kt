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

package com.example.android.architecture.blueprints.todoapp.tasks

import android.view.View
import android.widget.ListView
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.example.android.architecture.blueprints.todoapp.*
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the tasks screen, the main screen which contains a list of all tasks.
 */
@ExperimentalCoroutinesApi
@FlowPreview
@InternalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksScreenTest {

  /**
   * [ActivityTestRule] is a JUnit [Rule] to launch your activity under test.
   *
   *
   * Rules are interceptors which are executed for each test method and are important building
   * blocks of Junit tests.
   */
  @Suppress("MemberVisibilityCanPrivate")
  @get:Rule
  var tasksActivityTestRule: ActivityTestRule<TasksActivity> =
      object : ActivityTestRule<TasksActivity>(TasksActivity::class.java) {

        /**
         * To avoid a long list of tasks and the need to scroll through the list to find a
         * task, we call [TasksDataSource.deleteAllTasks] before each test.
         */
        override fun beforeActivityLaunched() = runBlockingTest {
          super.beforeActivityLaunched()
          // Doing this in @Before generates a race condition.
          Injection
              .provideTasksRepository(InstrumentationRegistry.getInstrumentation().targetContext)
              .deleteAllTasks()
        }
      }

  private val toolbarNavigationContentDescription: String
    get() = getToolbarNavigationContentDescription(tasksActivityTestRule.activity, R.id.toolbar)

  /**
   * A custom [Matcher] which matches an item in a [ListView] by its text.
   *
   *
   * View constraints:
   *
   *  * View must be a child of a [ListView]
   *
   *
   * @param itemText the text to match
   * @return Matcher that matches text in the given view
   */
  private fun withItemText(itemText: String): Matcher<View> {
    check(itemText.isNotEmpty()) { "itemText cannot be null or empty" }
    return object : TypeSafeMatcher<View>() {
      public override fun matchesSafely(item: View): Boolean {
        return allOf(isDescendantOfA(isAssignableFrom(ListView::class.java)), withText(itemText))
            .matches(item)
      }

      override fun describeTo(description: Description) {
        description.appendText("is isDescendantOfA LV with text " + itemText)
      }
    }
  }

  @Test
  fun clickAddTaskButton_opensAddTaskUi() {
    // Click on the add task button
    onView(withId(R.id.fab_add_task)).perform(click())

    // Check if the add task screen is displayed
    onView(withId(R.id.add_task_title)).check(matches(isDisplayed()))
  }

  @Test
  @Throws(Exception::class)
  fun editTask() {
    // First add a task
    createTask(TITLE1, DESCRIPTION)

    // Click on the task on the list
    onView(withText(TITLE1)).perform(click())

    // Click on the edit task button
    onView(withId(R.id.fab_edit_task)).perform(click())

    val editTaskTitle = TITLE2
    val editTaskDescription = "New Description"

    // Edit task title and description
    onView(withId(R.id.add_task_title))
        // Type new task title
        .perform(replaceText(editTaskTitle), closeSoftKeyboard())
    onView(withId(R.id.add_task_description))
        // Type new task description and close the keyboard
        .perform(replaceText(editTaskDescription), closeSoftKeyboard())

    // Save the task
    onView(withId(R.id.fab_edit_task_done)).perform(click())

    // Verify task is displayed on screen in the task list.
    onView(withItemText(editTaskTitle)).check(matches(isDisplayed()))

    // Verify previous task is not displayed
    onView(withItemText(TITLE1)).check(doesNotExist())
  }

  @Test
  @Throws(Exception::class)
  fun addTaskToTasksList() {
    createTask(TITLE1, DESCRIPTION)

    // Verify task is displayed on screen
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
  }

  @Test
  fun markTaskAsComplete() {
    viewAllTasks()

    // Add active task
    createTask(TITLE1, DESCRIPTION)

    // Mark the task as complete
    clickCheckBoxForTask(TITLE1)

    // Verify task is shown as complete
    viewAllTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    viewActiveTasks()
    onView(withItemText(TITLE1)).check(matches(not(isDisplayed())))
    viewCompletedTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
  }

  @Test
  fun markTaskAsActive() {
    viewAllTasks()

    // Add completed task
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)

    // Mark the task as active
    clickCheckBoxForTask(TITLE1)

    // Verify task is shown as active
    viewAllTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    viewActiveTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    viewCompletedTasks()
    onView(withItemText(TITLE1)).check(matches(not(isDisplayed())))
  }

  @Test
  fun showAllTasks() {
    // Add 2 active tasks
    createTask(TITLE1, DESCRIPTION)
    createTask(TITLE2, DESCRIPTION)

    //Verify that all our tasks are shown
    viewAllTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    onView(withItemText(TITLE2)).check(matches(isDisplayed()))
  }

  @Test
  fun showActiveTasks() {
    // Add 2 active tasks
    createTask(TITLE1, DESCRIPTION)
    createTask(TITLE2, DESCRIPTION)

    //Verify that all our tasks are shown
    viewActiveTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    onView(withItemText(TITLE2)).check(matches(isDisplayed()))
  }

  @Test
  fun showCompletedTasks() {
    // Add 2 completed tasks
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)
    createTask(TITLE2, DESCRIPTION)
    clickCheckBoxForTask(TITLE2)

    // Verify that all our tasks are shown
    viewCompletedTasks()
    onView(withItemText(TITLE1)).check(matches(isDisplayed()))
    onView(withItemText(TITLE2)).check(matches(isDisplayed()))
  }

  @Test
  fun clearCompletedTasks() {
    viewAllTasks()

    // Add 2 complete tasks
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)
    createTask(TITLE2, DESCRIPTION)
    clickCheckBoxForTask(TITLE2)

    // Click clear completed in menu
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.menu_clear)).perform(click())

    //Verify that completed tasks are not shown
    onView(withItemText(TITLE1)).check(matches(not(isDisplayed())))
    onView(withItemText(TITLE2)).check(matches(not(isDisplayed())))
  }

  @Test
  fun createOneTask_deleteTask() {
    viewAllTasks()

    // Add active task
    createTask(TITLE1, DESCRIPTION)

    // Open it in details view
    onView(withText(TITLE1)).perform(click())

    // Click delete task in menu
    onView(withId(R.id.menu_delete)).perform(click())

    // Verify it was deleted
    viewAllTasks()
    onView(withText(TITLE1)).check(matches(not(isDisplayed())))
  }

  @Test
  fun createTwoTasks_deleteOneTask() {
    // Add 2 active tasks
    createTask(TITLE1, DESCRIPTION)
    createTask(TITLE2, DESCRIPTION)

    // Open the second task in details view
    onView(withText(TITLE2)).perform(click())

    // Click delete task in menu
    onView(withId(R.id.menu_delete)).perform(click())

    // Verify only one task was deleted
    viewAllTasks()
    onView(withText(TITLE1)).check(matches(isDisplayed()))
    onView(withText(TITLE2)).check(doesNotExist())
  }

  @Test
  fun markTaskAsCompleteOnDetailScreen_taskIsCompleteInList() {
    viewAllTasks()

    // Add 1 active task
    createTask(TITLE1, DESCRIPTION)

    // Click on the task on the list
    onView(withText(TITLE1)).perform(click())

    // Click on the checkbox in task details screen
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click on the navigation up button to go back to the list
    onView(withContentDescription(toolbarNavigationContentDescription)).perform(click())

    // Check that the task is marked as completed
    onView(allOf(withId(R.id.complete), hasSibling(withText(TITLE1)))).check(matches(isChecked()))
  }

  @Test
  fun markTaskAsActiveOnDetailScreen_taskIsActiveInList() {
    viewAllTasks()

    // Add 1 completed task
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)

    // Click on the task on the list
    onView(withText(TITLE1)).perform(click())

    // Click on the checkbox in task details screen
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click on the navigation up button to go back to the list
    onView(withContentDescription(toolbarNavigationContentDescription)).perform(click())

    // Check that the task is marked as active
    onView(allOf(withId(R.id.complete), hasSibling(withText(TITLE1))))
        .check(matches(not(isChecked())))
  }

  @Test
  fun markTaskAsAcompleteAndActiveOnDetailScreen_taskIsActiveInList() {
    viewAllTasks()

    // Add 1 active task
    createTask(TITLE1, DESCRIPTION)

    // Click on the task on the list
    onView(withText(TITLE1)).perform(click())

    // Click on the checkbox in task details screen
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click again to restore it to original state
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click on the navigation up button to go back to the list
    onView(withContentDescription(toolbarNavigationContentDescription)).perform(click())

    // Check that the task is marked as active
    onView(allOf(withId(R.id.complete), hasSibling(withText(TITLE1))))
        .check(matches(not(isChecked())))
  }

  @Test
  fun markTaskAsActiveAndCompleteOnDetailScreen_taskIsCompleteInList() {
    viewAllTasks()

    // Add 1 completed task
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)

    // Click on the task on the list
    onView(withText(TITLE1)).perform(click())

    // Click on the checkbox in task details screen
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click again to restore it to original state
    onView(withId(R.id.task_detail_complete)).perform(click())

    // Click on the navigation up button to go back to the list
    onView(withContentDescription(toolbarNavigationContentDescription)).perform(click())

    // Check that the task is marked as active
    onView(allOf(withId(R.id.complete), hasSibling(withText(TITLE1)))).check(matches(isChecked()))
  }

  @Test
  fun orientationChange_FilterActivePersists() {

    // Add a completed task
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)

    // when switching to active tasks
    viewActiveTasks()

    // then no tasks should appear
    onView(withText(TITLE1)).check(matches(not(isDisplayed())))

    // when rotating the screen
    rotateOrientation(tasksActivityTestRule.activity)

    // then nothing changes
    onView(withText(TITLE1)).check(doesNotExist())
  }

  @Test
  fun orientationChange_FilterCompletedPersists() {

    // Add a completed task
    createTask(TITLE1, DESCRIPTION)
    clickCheckBoxForTask(TITLE1)

    // when switching to completed tasks
    viewCompletedTasks()

    // the completed task should be displayed
    onView(withText(TITLE1)).check(matches(isDisplayed()))

    // when rotating the screen
    rotateOrientation(tasksActivityTestRule.activity)

    // then nothing changes
    onView(withText(TITLE1)).check(matches(isDisplayed()))
    onView(withText(R.string.label_completed)).check(matches(isDisplayed()))
  }

  @Test
  @SdkSuppress(minSdkVersion = 21) // Blinking cursor after rotation breaks this in API 19
  @Throws(Throwable::class)
  fun orientationChange_DuringEdit_ChangePersists() {
    // Add a completed task
    createTask(TITLE1, DESCRIPTION)

    // Open the task in details view
    onView(withText(TITLE1)).perform(click())

    // Click on the edit task button
    onView(withId(R.id.fab_edit_task)).perform(click())

    // Change task title (but don't save)
    onView(withId(R.id.add_task_title))
        // Type new task title
        .perform(replaceText(TITLE2), closeSoftKeyboard())

    // Rotate the screen
    rotateOrientation(getCurrentActivity())

    // Verify task title is restored
    onView(withId(R.id.add_task_title)).check(matches(withText(TITLE2)))
  }

  @Test
  @SdkSuppress(minSdkVersion = 21) // Blinking cursor after rotation breaks this in API 19
  @Throws(IllegalStateException::class)
  fun orientationChange_DuringEdit_NoDuplicate() {
    // Add a completed task
    createTask(TITLE1, DESCRIPTION)

    // Open the task in details view
    onView(withText(TITLE1)).perform(click())

    // Click on the edit task button
    onView(withId(R.id.fab_edit_task)).perform(click())

    // Rotate the screen
    rotateOrientation(getCurrentActivity())

    // Edit task title and description
    onView(withId(R.id.add_task_title))
        // Type new task title
        .perform(replaceText(TITLE2), closeSoftKeyboard())
    onView(withId(R.id.add_task_description))
        // Type new task description and close the keyboard
        .perform(replaceText(DESCRIPTION), closeSoftKeyboard())

    // Save the task
    onView(withId(R.id.fab_edit_task_done)).perform(click())

    // Verify task is displayed on screen in the task list.
    onView(withItemText(TITLE2)).check(matches(isDisplayed()))

    // Verify previous task is not displayed
    onView(withItemText(TITLE1)).check(doesNotExist())
  }

  private fun viewAllTasks() {
    onView(withId(R.id.menu_filter)).perform(click())
    onView(withText(R.string.nav_all)).perform(click())
  }

  private fun viewActiveTasks() {
    onView(withId(R.id.menu_filter)).perform(click())
    onView(withText(R.string.nav_active)).perform(click())
  }

  private fun viewCompletedTasks() {
    onView(withId(R.id.menu_filter)).perform(click())
    onView(withText(R.string.nav_completed)).perform(click())
  }

  private fun createTask(title: String, description: String) {
    // Click on the add task button
    onView(withId(R.id.fab_add_task)).perform(click())

    // Add task title and description
    onView(withId(R.id.add_task_title))
        // Type new task title
        .perform(typeText(title), closeSoftKeyboard())
    onView(withId(R.id.add_task_description))
        // Type new task description and close the keyboard
        .perform(typeText(description), closeSoftKeyboard())

    // Save the task
    onView(withId(R.id.fab_edit_task_done)).perform(click())
  }

  private fun clickCheckBoxForTask(title: String) {
    onView(allOf(withId(R.id.complete), hasSibling(withText(title)))).perform(click())
  }

  companion object {
    private const val TITLE1 = "TITLE1"
    private const val DESCRIPTION = "DESCR"
    private const val TITLE2 = "TITLE2"
  }
}
