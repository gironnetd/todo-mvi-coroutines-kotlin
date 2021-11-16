package com.example.android.architecture.blueprints.todoapp.util

fun String?.isNullOrEmpty() = this == null || this.isEmpty()
fun String?.isNotNullNorEmpty() = !this.isNullOrEmpty()