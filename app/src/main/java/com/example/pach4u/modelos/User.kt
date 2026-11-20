package com.example.pach4u.modelos

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val position: String = "MED",
    val preferredFoot: String = "DERECHO",
    val skillLevel: Double = 5.0
)