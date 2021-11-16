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

package com.example.android.architecture.blueprints.todoapp.addedittask

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.addFragmentToActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * Displays an add or edit task screen.
 */

@FlowPreview
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class AddEditTaskActivity : AppCompatActivity() {

  private lateinit var actionBar: ActionBar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.addtask_act)

    // Set up the toolbar.
    setSupportActionBar(findViewById(R.id.toolbar))
    supportActionBar?.run {
      actionBar = this
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }

    val taskId = intent.getStringExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID)
    setToolbarTitle(taskId)

    if (supportFragmentManager.findFragmentById(R.id.contentFrame) == null) {
      val addEditTaskFragment = AddEditTaskFragment.invoke()

      if (taskId != null) {
        val args = Bundle()
        args.putString(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID, taskId)
        addEditTaskFragment.arguments = args
      }

      addFragmentToActivity(supportFragmentManager, addEditTaskFragment, R.id.contentFrame)
    }
  }

  private fun setToolbarTitle(taskId: String?) {
    actionBar.setTitle(if (taskId == null) R.string.add_task else R.string.edit_task)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  companion object {
    const val REQUEST_ADD_TASK = 1
  }
}
