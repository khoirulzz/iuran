package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.domain.Officer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOfficersScreen(
    repository: AppRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var officers by remember { mutableStateOf<List<Officer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedOfficer by remember { mutableStateOf<Officer?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    fun loadOfficers() {
        coroutineScope.launch {
            isLoading = true
            officers = repository.getOfficers()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadOfficers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akun Petugas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Tambah Petugas", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
        ) {
            if (successMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = OfficerPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(successMsg!!, color = OfficerDark, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                }
            } else if (officers.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SupervisedUserCircle, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Belum ada akun petugas", color = TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                        ) { Text("Tambah Petugas") }
                    }
                }
            } else {
                item {
                    Text(
                        "${officers.size} petugas terdaftar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(officers) { officer ->
                    OfficerListItem(
                        officer = officer,
                        onResetPassword = {
                            selectedOfficer = officer
                            showResetDialog = true
                        },
                        onToggleActive = {
                            coroutineScope.launch {
                                repository.setOfficerActiveStatus(officer.id, !officer.isActive)
                                loadOfficers()
                            }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Dialog Tambah Petugas
    if (showAddDialog) {
        AddOfficerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, username, password, phone ->
                coroutineScope.launch {
                    val result = repository.createOfficer(name, username, password, phone)
                    if (result.isSuccess) {
                        successMsg = "Akun '$username' berhasil dibuat."
                        loadOfficers()
                    } else {
                        successMsg = result.exceptionOrNull()?.message ?: "Gagal membuat akun."
                    }
                    showAddDialog = false
                }
            }
        )
    }

    // Dialog Reset Password
    if (showResetDialog && selectedOfficer != null) {
        ResetPasswordDialog(
            officerName = selectedOfficer!!.name,
            onDismiss = { showResetDialog = false; selectedOfficer = null },
            onReset = { newPassword ->
                coroutineScope.launch {
                    repository.resetOfficerPassword(selectedOfficer!!.id, newPassword)
                    successMsg = "Password ${selectedOfficer!!.name} berhasil direset."
                    showResetDialog = false
                    selectedOfficer = null
                }
            }
        )
    }
}

@Composable
private fun OfficerListItem(
    officer: Officer,
    onResetPassword: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (officer.isActive) AppSurface else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (officer.isActive) AdminContainer else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    officer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (officer.isActive) AdminPrimary else TextDisabled
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        officer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (officer.isActive) TextPrimary else TextDisabled
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (officer.isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (officer.isActive) "Aktif" else "Nonaktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (officer.isActive) OfficerPrimary else TextDisabled
                        )
                    }
                }
                Text(
                    "@${officer.username} · ${officer.phone.ifEmpty { "No. HP belum diisi" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column {
                IconButton(onClick = onResetPassword, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.LockReset, "Reset Password", tint = AdminPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleActive, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (officer.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                        if (officer.isActive) "Nonaktifkan" else "Aktifkan",
                        tint = if (officer.isActive) AccentDanger else OfficerPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOfficerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, username: String, password: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Tambah Akun Petugas", fontWeight = FontWeight.Bold, color = AdminPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().replace(" ", "") },
                    label = { Text("Username *") },
                    placeholder = { Text("min. 4 karakter, tanpa spasi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    placeholder = { Text("min. 6 karakter") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = AccentDanger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMsg = "Nama wajib diisi."
                        username.length < 4 -> errorMsg = "Username minimal 4 karakter."
                        username.contains(" ") -> errorMsg = "Username tidak boleh mengandung spasi."
                        password.length < 6 -> errorMsg = "Password minimal 6 karakter."
                        else -> onSave(name.trim(), username.trim(), password, phone.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Buat Akun") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordDialog(
    officerName: String,
    onDismiss: () -> Unit,
    onReset: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = AccentDanger) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reset password untuk akun: $officerName", color = TextSecondary)
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru *") },
                    placeholder = { Text("min. 6 karakter") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
                if (errorMsg != null) Text(errorMsg!!, color = AccentDanger, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 6) errorMsg = "Password minimal 6 karakter."
                    else onReset(newPassword)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}
