package com.omega.ordencompra.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omega.ordencompra.R
import com.omega.ordencompra.ui.theme.PrimaryBlue

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit,
    isLoading: Boolean,
    error: String?,
    initialEmail: String = "",
    initialPassword: String = "",
    initialRememberMe: Boolean = false
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(initialRememberMe) }

    val deepBlue = PrimaryBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(150.dp)
        )
        Spacer(Modifier.height(24.dp))

        Text(
            "Ingresa tus credenciales para acceder a tu panel de control.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            placeholder = { Text("nombre@empresa.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = deepBlue) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            placeholder = { Text("\u2022".repeat(8)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = deepBlue) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Spacer(Modifier.width(4.dp))
                Text("Recordarme", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "¿Olvidaste tu contraseña?",
                color = deepBlue,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { }
            )
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { onLogin(email, password, rememberMe) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = deepBlue)
        ) {
            if (isLoading) {
                Text("Iniciando sesión...")
            } else {
                Text("Iniciar Sesión", fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(24.dp))

        Text(
            "¿No tienes cuenta? Contacta a tu administrador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        Text(
            "Protocolo SSL 256-bit  |  Acceso Global Seguro",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Soporte", style = MaterialTheme.typography.labelSmall, color = deepBlue)
            Text("  |  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Privacidad", style = MaterialTheme.typography.labelSmall, color = deepBlue)
            Text("  |  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Términos", style = MaterialTheme.typography.labelSmall, color = deepBlue)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "\u00A9 2026 Navix System. Todos los derechos reservados.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}
