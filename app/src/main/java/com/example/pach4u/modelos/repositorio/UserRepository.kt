package com.example.pach4u.modelos.repositorio

import com.example.pach4u.modelos.local.UserDao
import com.example.pach4u.modelos.User
import com.example.pach4u.modelos.remoto.FirebaseUserService
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val firebaseService: FirebaseUserService
) {

    fun getUserFlow(uid: String): Flow<User?> = userDao.getUserFlow(uid)

    suspend fun syncOrCreateUser(uid: String, defaultName: String) {
        val remoteUser = firebaseService.getUser(uid)
        val userToSave = remoteUser ?: User(
            uid = uid,
            name = defaultName.ifBlank { "Jugador" },
            position = "MED",
            preferredFoot = "DERECHO",
            skillLevel = 5.0
        ).also { created ->
            firebaseService.saveUser(created)
        }
        userDao.insertUser(userToSave)
    }

    suspend fun updateUser(user: User): Result<Unit> {
        userDao.insertUser(user)

        return runCatching {
            firebaseService.saveUser(user)
        }
    }
}