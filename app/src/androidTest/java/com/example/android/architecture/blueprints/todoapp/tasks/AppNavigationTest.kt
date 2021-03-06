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

import android.view.Gravity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.custom.action.NavigationViewActions.navigateTo
import com.example.android.architecture.blueprints.todoapp.getToolbarNavigationContentDescription
import junit.framework.Assert.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the [DrawerLayout] layout component in [TasksActivity] which manages
 * navigation within the app.
 */
@FlowPreview
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {

  /**
   * [ActivityTestRule] is a JUnit [@Rule][Rule] to launch your activity under test.
   *
   *
   *
   *
   * Rules are interceptors which are executed for each test method and are important building
   * blocks of Junit tests.
   */
  @get:Rule
  var activityTestRule = ActivityTestRule(TasksActivity::class.java)

  @Test
  fun clickOnStatisticsNavigationItem_ShowsStatisticsScreen() {
    openStatisticsScreen()

    // Check that statistics Activity was opened.
    onView(withId(R.id.statistics)).check(matches(isDisplayed()))
  }

  @Test
  fun clickOnListNavigationItem_ShowsListScreen() {
    openStatisticsScreen()

    openTasksScreen()

    // Check that Tasks Activity was opened.
    onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()))
  }

  @Test
  fun clickOnAndroidHomeIcon_OpensNavigation() {
    // Check that left drawer is closed at startup
    onView(withId(R.id.drawer_layout))
        // Left Drawer should be closed.
        .check(matches(isClosed(Gravity.LEFT)))

    // Open Drawer
    onView(
        withContentDescription(
            getToolbarNavigationContentDescription(activityTestRule.activity, R.id.toolbar))
    ).perform(click())

    // Check if drawer is open
    onView(withId(R.id.drawer_layout))
        // Left drawer is open.
        .check(matches(isOpen(Gravity.LEFT)))
  }


  @Test
  fun Statistics_backNavigatesToTasks() {
    openStatisticsScreen()

    // Press back to go back to the tasks list
    pressBack()

    // Check that Tasks Activity was restored.
    onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()))
  }

  @Test
  fun backFromTasksScreen_ExitsApp() {
    // From the tasks screen, press back should exit the app.
    assertPressingBackExitsApp()
  }

  @Test
  fun backFromTasksScreenAfterStats_ExitsApp() {
    // This test checks that TasksActivity is a parent of StatisticsActivity

    // Open the stats screen
    openStatisticsScreen()

    // Open the tasks screen to restore the task
    openTasksScreen()

    // Pressing back should exit app
    assertPressingBackExitsApp()
  }

  private fun assertPressingBackExitsApp() {
    try {
      pressBack()
      fail("Should kill the app and throw an exception")
    } catch (e: NoActivityResumedException) {
      // Test OK
    }

  }

  private fun openTasksScreen() {
    // Open Drawer to click on navigation item.
    onView(withId(R.id.drawer_layout))
        // Left Drawer should be closed.
        .check(matches(isClosed(Gravity.LEFT)))
        // Open Drawer
        .perform(open())

    // Start tasks list screen.
    onView(withId(R.id.nav_view))
        .perform(navigateTo(R.id.list_navigation_menu_item))
  }

  private fun openStatisticsScreen() {
    // Open Drawer to click on navigation item.
    onView(withId(R.id.drawer_layout))
        // Left Drawer should be closed.
        .check(matches(isClosed(Gravity.LEFT)))
        // Open Drawer
        .perform(open())

    // Start statistics screen.
    onView(withId(R.id.nav_view))
        .perform(navigateTo(R.id.statistics_navigation_menu_item))
  }
}
