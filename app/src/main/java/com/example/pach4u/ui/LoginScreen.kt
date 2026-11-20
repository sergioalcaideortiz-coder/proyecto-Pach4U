package com.example.pach4u.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    webClientId: String = "1039427877838-6o3rk9mml5ron2cb8u0qm4bpo1le5em5.apps.googleusercontent.com",
    onAuthSuccess: (uid: String, defaultName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun signInWithEmail() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = if (isRegisterMode) {
                    auth.createUserWithEmailAndPassword(email.trim(), password).await()
                } else {
                    auth.signInWithEmailAndPassword(email.trim(), password).await()
                }
                val user = result.user ?: error("No se obtuvo el usuario")
                val displayName = name.trim().ifBlank {
                    user.displayName ?: email.substringBefore("@")
                }
                onAuthSuccess(user.uid, displayName)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Error en autenticación"
            } finally {
                isLoading = false
            }
        }
    }

    fun signInWithGoogle() {
        val activity = context as? Activity ?: return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(activity, request)
                val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)

                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: error("Error al iniciar sesión con Google")
                onAuthSuccess(user.uid, user.displayName ?: googleCredential.displayName ?: "Jugador")
            } catch (e: GetCredentialException) {
                errorMessage = e.localizedMessage ?: "Selección de cuenta cancelada"
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Error con Google Sign-In"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        if (isRegisterMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { signInWithEmail() },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isLoading) "Cargando..."
                else if (isRegisterMode) "Registrarse"
                else "Entrar"
            )
        }

        OutlinedButton(
            onClick = { signInWithGoogle() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar con Google")
        }

        TextButton(
            onClick = { isRegisterMode = !isRegisterMode },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión"
                else "¿No tienes cuenta? Regístrate"
            )
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}