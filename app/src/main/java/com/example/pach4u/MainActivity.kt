package com.example.pach4u

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pach4u.modelos.local.AppDatabase
import com.example.pach4u.modelos.remoto.FirebaseUserService
import com.example.pach4u.modelos.repositorio.UserRepository
import com.example.pach4u.ui.LoginScreen
import com.example.pach4u.ui.ProfileScreen
import com.example.pach4u.ui.theme.Pach4UTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()
        val database = AppDatabase.getDatabase(applicationContext)
        val firebaseService = FirebaseUserService()
        val repository = UserRepository(
            userDao = database.userDao(),
            firebaseService = firebaseService
        )

        setContent {
            Pach4UTheme {
                val scope = rememberCoroutineScope()
                var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }
                var isSyncingUser by remember { mutableStateOf(false) }

                LaunchedEffect (currentUid) {
                    val uid = currentUid
                    if (uid != null) {
                        isSyncingUser = true
                        val defaultName = auth.currentUser?.displayName
                            ?: auth.currentUser?.email?.substringBefore("@")
                            ?: "Jugador"
                        runCatching {
                            repository.syncOrCreateUser(uid, defaultName)
                        }
                        isSyncingUser = false
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when {
                            currentUid == null -> {
                                LoginScreen (
                                    auth = auth,
                                    onAuthSuccess = { uid, defaultName ->
                                        scope.launch {
                                            isSyncingUser = true
                                            runCatching {
                                                repository.syncOrCreateUser(uid, defaultName)
                                            }
                                            currentUid = uid
                                            isSyncingUser = false
                                        }
                                    }
                                )
                            }

                            isSyncingUser -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            else -> {
                                ProfileScreen (
                                    uid = currentUid!!,
                                    repository = repository,
                                    onLogout = {
                                        auth.signOut()
                                        currentUid = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
