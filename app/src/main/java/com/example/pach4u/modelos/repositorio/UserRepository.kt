package com.example.pach4u.modelos.repositorio

import com.example.pach4u.modelos.User
import com.example.pach4u.modelos.local.UserDao
import com.example.pach4u.modelos.remoto.FirebaseUserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

class UserRepository(
    private val userDao: UserDao,
    private val firebaseService: FirebaseUserService
) {

    fun getUserFlow(uid: String): Flow<User?> = userDao.getUserFlow(uid)

    suspend fun getLocalUser(uid: String): User? = userDao.getUserById(uid)

    suspend fun syncOrCreateUser(uid: String, defaultName: String) {
        val localUser = userDao.getUserById(uid)

        if (localUser != null && localUser.pendingSync) {
            pushPendingChanges(uid)
            return
        }

        val remoteUser = withTimeoutOrNull(3000L) {
            runCatching { firebaseService.getUser(uid) }.getOrNull()
        }

        when {
            remoteUser != null -> {
                userDao.insertUser(remoteUser.copy(uid = uid, pendingSync = false))
            }
            localUser == null -> {
                val created = User(
                    uid = uid,
                    name = defaultName.ifBlank { "Jugador" },
                    position = "MED",
                    preferredFoot = "DERECHO",
                    skillLevel = 5.0,
                    pendingSync = true
                )
                userDao.insertUser(created)
                val uploaded = uploadToFirebase(created)
                if (uploaded) {
                    userDao.insertUser(created.copy(pendingSync = false))
                }
            }
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        val pendingUser = user.copy(pendingSync = true)
        userDao.insertUser(pendingUser)

        val uploaded = uploadToFirebase(pendingUser)
        return if (uploaded) {
            userDao.insertUser(pendingUser.copy(pendingSync = false))
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Sin conexión; guardado en Room pendiente de sincronizar"))
        }
    }

    suspend fun pushPendingChanges(uid: String) {
        val localUser = userDao.getUserById(uid) ?: return
        if (!localUser.pendingSync) return

        if (uploadToFirebase(localUser)) {
            userDao.insertUser(localUser.copy(pendingSync = false))
        }
    }

    private suspend fun uploadToFirebase(user: User): Boolean {
        return withTimeoutOrNull(4000L) {
            runCatching {
                firebaseService.saveUser(user.copy(pendingSync = false))
                true
            }.getOrDefault(false)
        } ?: false
    }
}