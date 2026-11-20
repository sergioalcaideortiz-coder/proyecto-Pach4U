package com.example.pach4u.modelos.remoto

import com.google.firebase.database.FirebaseDatabase
import com.example.pach4u.modelos.User
import kotlinx.coroutines.tasks.await

class FirebaseUserService(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private fun userRef(uid: String) = db.getReference("users").child(uid)

    suspend fun getUser(uid: String): User? {
        val snapshot = userRef(uid).get().await()
        return snapshot.getValue(User::class.java)?.copy(uid = uid)
    }

    suspend fun saveUser(user: User) {
        userRef(user.uid).setValue(user).await()
    }
}