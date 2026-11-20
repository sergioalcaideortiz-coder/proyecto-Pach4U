package com.example.pach4u.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pach4u.modelos.User
import com.example.pach4u.modelos.repositorio.UserRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    uid: String,
    repository: UserRepository,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val user by repository.getUserFlow(uid).collectAsState(initial = null)

    val positions = listOf("POR", "DEF", "MED", "DEL")
    val feet = listOf("DERECHO", "IZQUIERDO", "AMBOS")

    var name by remember(user?.uid) { mutableStateOf(user?.name.orEmpty()) }
    var position by remember(user?.uid) { mutableStateOf(user?.position ?: "MED") }
    var preferredFoot by remember(user?.uid) { mutableStateOf(user?.preferredFoot ?: "DERECHO") }
    var skillSlider by remember(user?.uid) { mutableFloatStateOf((user?.skillLevel ?: 5.0).toFloat()) }

    var isSaving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        user?.let {
            name = it.name
            position = it.position
            preferredFoot = it.preferredFoot
            skillSlider = it.skillLevel.toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Posición", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            positions.forEach { pos ->
                FilterChip(
                    selected = position == pos,
                    onClick = { position = pos },
                    label = { Text(pos) }
                )
            }
        }

        Text("Pie bueno", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            feet.forEach { foot ->
                FilterChip(
                    selected = preferredFoot == foot,
                    onClick = { preferredFoot = foot },
                    label = { Text(foot) }
                )
            }
        }

        val roundedSkill = ((skillSlider * 10.0).roundToInt() / 10.0)
        Text("Nivel: $roundedSkill", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = skillSlider,
            onValueChange = { skillSlider = it },
            valueRange = 1f..10f
        )

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    feedback = null
                    val updated = User(
                        uid = uid,
                        name = name.trim(),
                        position = position,
                        preferredFoot = preferredFoot,
                        skillLevel = roundedSkill
                    )
                    val result = repository.updateUser(updated)
                    feedback = if (result.isSuccess) {
                        "Guardado en local (Room 3) y en Firebase"
                    } else {
                        "Guardado en local; pendiente de subir a Firebase"
                    }
                    isSaving = false
                }
            },
            enabled = !isSaving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Guardando..." else "Guardar cambios")
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }

        feedback?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }
    }
}