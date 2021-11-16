/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.statistics.StatisticsActivity
import com.example.android.architecture.blueprints.todoapp.util.addFragmentToActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi

@FlowPreview
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class TasksActivity : AppCompatActivity() {
  private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tasks_act)

    // Set up the toolbar.
    setSupportActionBar(findViewById(R.id.toolbar))
    supportActionBar!!.run {
      setHomeAsUpIndicator(R.drawable.ic_menu)
      setDisplayHomeAsUpEnabled(true)
    }

    // Set up the navigation drawer.
    drawerLayout = findViewById(R.id.drawer_layout)
    drawerLayout.setStatusBarBackground(R.color.colorPrimaryDark)
    val navigationView = findViewById<NavigationView>(R.id.nav_view)
    if (navigationView != null) {
      setupDrawerContent(navigationView)
    }

    if (supportFragmentManager.findFragmentById(R.id.contentFrame) == null) {
      addFragmentToActivity(supportFragmentManager, TasksFragment(), R.id.contentFrame)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        // Open the navigation drawer when the home icon is selected from the toolbar.
        drawerLayout.openDrawer(GravityCompat.START)
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setupDrawerContent(navigationView: NavigationView) {
    navigationView.setNavigationItemSelectedListener { menuItem ->
      when (menuItem.itemId) {
        R.id.list_navigation_menu_item -> {
          // Do nothing, we're already on that screen
        }
        R.id.statistics_navigation_menu_item -> {
          val intent = Intent(this@TasksActivity, StatisticsActivity::class.java)
          startActivity(intent)
        }
        else -> {
        }
      }
      // Close the navigation drawer when an item is selected.
      menuItem.isChecked = true
      drawerLayout.closeDrawers()
      true
    }
  }
}
