package com.example.pach4u.modelos

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.firebase.database.Exclude

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val position: String = "MED",
    val preferredFoot: String = "DERECHO",
    val skillLevel: Double = 5.0,
    @get:Exclude val pendingSync: Boolean = false
)