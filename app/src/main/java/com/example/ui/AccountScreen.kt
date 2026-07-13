package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SessionStore
import com.example.ui.theme.*

/**
 * Tab Akun yang dapat dipakai oleh Admin maupun Petugas.
 * primaryColor: warna tema (AdminPrimary untuk admin, OfficerPrimary untuk petugas)
 * onLogout: callback ketika user memilih logout — handle konfirmasi di pemanggil.
 */
@Composable
fun AccountScreen(
    primaryColor: androidx.compose.ui.graphics.Color,
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    sessionStore: SessionStore? = null
) {
    var officerName by remember { mutableStateOf("") }
    var officerId by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sessionStore?.userName?.collect { name -> name?.let { officerName = it } }
    }
    LaunchedEffect("accountId") {
        sessionStore?.userId?.collect { id -> id?.let { officerId = it } }
    }
    LaunchedEffect("accountRole") {
        sessionStore?.userRole?.collect { role -> role?.let { userRole = it } }
    }

    val displayRole = when (userRole.uppercase()) {
        "ADMIN" -> "Administrator"
        "OFFICER" -> "Petugas"
        else -> "Pengguna"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            officerName.take(1).uppercase().ifEmpty { "A" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            officerName.ifEmpty { "Pengguna" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            displayRole,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("ID Pengguna", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                officerId.ifEmpty { "Memuat…" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Badge, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = AppSurface)
                    )
                    HorizontalDivider(color = BorderColor)
                    ListItem(
                        headlineContent = { Text("Role", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(displayRole, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        },
                        leadingContent = { Icon(Icons.Default.Work, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = AppSurface)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onLogout() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Keluar Akun",
                            fontWeight = FontWeight.SemiBold,
                            color = AccentDanger
                        )
                    },
                    supportingContent = {
                        Text(
                            "Sesi Anda akan diakhiri",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentDanger.copy(alpha = 0.7f)
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Logout, null, tint = AccentDanger)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Informasi Aplikasi & Pengembang
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Gempala Iuran · Offline-First Edition",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Versi v1.0.0 (Production Release)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pengembang Aplikasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabled
                    )
                    Text(
                        "Muhamad Khoirul Ulum",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
