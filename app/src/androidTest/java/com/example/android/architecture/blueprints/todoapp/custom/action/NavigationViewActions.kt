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

package com.example.android.architecture.blueprints.todoapp.custom.action

import android.content.res.Resources.NotFoundException
import android.view.Menu
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * View actions for interacting with [NavigationView]
 */
object NavigationViewActions {
  /**
   * Returns a [ViewAction] that navigates to a menu item in [NavigationView] using a
   * menu item resource id.
   *
   *
   *
   *
   * View constraints:
   *
   *  * View must be a child of a [DrawerLayout]
   *  * View must be of type [NavigationView]
   *  * View must be visible on screen
   *  * View must be displayed on screen
   *
   *
   * @param menuItemId the resource id of the menu item
   * @return a [ViewAction] that navigates on a menu item
   */
  fun navigateTo(menuItemId: Int): ViewAction {
    return object : ViewAction {
      override fun perform(uiController: UiController, view: View) {
        val navigationView = view as NavigationView
        val menu = navigationView.menu
        if (null == menu.findItem(menuItemId)) {
          throw PerformException.Builder()
              .withActionDescription(this.description)
              .withViewDescription(HumanReadables.describe(view))
              .withCause(RuntimeException(getErrorMessage(menu, view)))
              .build()
        }
        menu.performIdentifierAction(menuItemId, 0)
        uiController.loopMainThreadUntilIdle()
      }

      private fun getErrorMessage(menu: Menu, view: View): String {
        val NEW_LINE = System.getProperty("line.separator")
        val errorMessage = StringBuilder("Menu item was not found, " + "available menu items:")
            .append(NEW_LINE)
        for (position in 0 until menu.size()) {
          errorMessage.append("[MenuItem] position=").append(position)
          val menuItem = menu.getItem(position)
          if (menuItem != null) {
            val itemTitle = menuItem.title
            if (itemTitle != null) {
              errorMessage.append(", title=").append(itemTitle)
            }
            if (view.resources != null) {
              val itemId = menuItem.itemId
              try {
                errorMessage.append(", id=")
                val menuItemResourceName = view.resources.getResourceName(itemId)
                errorMessage.append(menuItemResourceName)
              } catch (nfe: NotFoundException) {
                errorMessage.append("not found")
              }

            }
            errorMessage.append(NEW_LINE)
          }
        }
        return errorMessage.toString()
      }

      override fun getDescription(): String {
        return "click on menu item with id"
      }

      override fun getConstraints(): Matcher<View> {
        return allOf(isAssignableFrom(NavigationView::class.java),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), isDisplayingAtLeast(90))
      }
    }
  }
}
