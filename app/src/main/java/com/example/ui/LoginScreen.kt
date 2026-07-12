package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.data.SessionStore
import com.example.data.AppRepository
import com.example.domain.UserRole
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    sessionStore: SessionStore,
    repository: AppRepository
) {
    var isOfficer by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val primaryColor = if (isOfficer) OfficerPrimary else AdminPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = "Logo Gempala Iuran",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Gempala Iuran",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pencatatan iuran kegiatan warga",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Segmented Pilihan: Admin / Petugas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false, true).forEach { officer ->
                    val selected = isOfficer == officer
                    val color = if (officer) OfficerPrimary else AdminPrimary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) color else Color.Transparent)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { isOfficer = officer },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) color else Color.Transparent,
                                contentColor = if (selected) Color.White else TextSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (officer) "Petugas" else "Admin",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(if (isOfficer) "Username Petugas" else "Email Admin") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = BorderColor
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Sembunyikan" else "Tampilkan",
                        tint = TextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = BorderColor
            )
        )

        // Error
        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = error!!,
                    color = AccentDanger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!isOfficer) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Login pertama petugas membutuhkan internet.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) return@Button
                coroutineScope.launch {
                    loading = true
                    error = null
                    try {
                        if (isOfficer) {
                            val officer = repository.loginOfficer(username, password).getOrThrow()
                            sessionStore.saveSession(UserRole.OFFICER.name, officer.id, officer.name)
                            navController.navigate("officer_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            if (username.trim().lowercase() == "admin@gempala.com" && password == "gempala2026") {
                                sessionStore.saveSession(UserRole.ADMIN.name, "admin", "Admin")
                                navController.navigate("admin_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                error = "Email atau password admin tidak sesuai."
                            }
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Login gagal. Periksa koneksi internet."
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Masuk", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
