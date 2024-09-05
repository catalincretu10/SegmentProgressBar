package com.example.testapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
@Parcelize
data class Student (val user: String, val otherUser: String) : Parcelable