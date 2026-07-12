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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.domain.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityFormScreen(
    activityId: String?, // null or "new" = buat baru, lainnya = edit
    navController: NavController,
    repository: AppRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val isNew = activityId.isNullOrBlank() || activityId == "new"

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var allowLatePayment by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf(ActivityStatus.DRAFT) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Date pickers (simplified: teks string format yyyy-MM-dd)
    var startDateStr by remember { mutableStateOf("") }
    var deadlineDateStr by remember { mutableStateOf("") }

    var existingActivity by remember { mutableStateOf<IuranActivity?>(null) }

    // Load existing if edit
    LaunchedEffect(activityId) {
        if (!isNew && activityId != null) {
            val act = repository.getActivityById(activityId)
            if (act != null) {
                existingActivity = act
                name = act.name
                description = act.description
                targetAmount = act.defaultTargetAmount.toString()
                allowLatePayment = act.allowLatePayment
                status = act.status
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                startDateStr = if (act.startAtEpochMs > 0) sdf.format(Date(act.startAtEpochMs)) else ""
                deadlineDateStr = if (act.deadlineAtEpochMs > 0) sdf.format(Date(act.deadlineAtEpochMs)) else ""
            }
        }
    }

    fun parseDate(str: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
            sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdf.parse(str)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "Tambah Kegiatan" else "Edit Kegiatan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FormSection(title = "Informasi Kegiatan") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Kegiatan *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMsg != null && name.isBlank(),
                        leadingIcon = { Icon(Icons.Default.Event, null, tint = AdminPrimary) }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            item {
                FormSection(title = "Tanggal & Target") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).clickable {
                            val cal = Calendar.getInstance()
                            val dialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    startDateStr = String.format(Locale("id", "ID"), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.datePicker.minDate = cal.timeInMillis
                            dialog.show()
                        }) {
                            OutlinedTextField(
                                value = startDateStr,
                                onValueChange = { },
                                label = { Text("Tanggal Mulai *") },
                                placeholder = { Text("dd/MM/yyyy") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            )
                        }
                        Box(modifier = Modifier.weight(1f).clickable {
                            val cal = Calendar.getInstance()
                            val dialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    deadlineDateStr = String.format(Locale("id", "ID"), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.datePicker.minDate = cal.timeInMillis
                            dialog.show()
                        }) {
                            OutlinedTextField(
                                value = deadlineDateStr,
                                onValueChange = { },
                                label = { Text("Tenggat *") },
                                placeholder = { Text("dd/MM/yyyy") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { if (it.all(Char::isDigit)) targetAmount = it },
                        label = { Text("Target Default per Warga (Rp) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("Rp", color = AdminPrimary, modifier = Modifier.padding(start = 12.dp)) }
                    )
                }
            }

            item {
                FormSection(title = "Pengaturan") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allowLatePayment,
                            onCheckedChange = { allowLatePayment = it },
                            colors = CheckboxDefaults.colors(checkedColor = AdminPrimary)
                        )
                        Text(
                            "Izinkan pembayaran setelah tenggat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Status Awal", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = status == ActivityStatus.DRAFT,
                            onClick = { status = ActivityStatus.DRAFT },
                            label = { Text("Draft") }
                        )
                        FilterChip(
                            selected = status == ActivityStatus.ACTIVE,
                            onClick = { status = ActivityStatus.ACTIVE },
                            label = { Text("Aktif") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminLight,
                                selectedLabelColor = AdminPrimary
                            )
                        )
                    }
                }
            }

            if (errorMsg != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = AccentDanger, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMsg!!, color = AccentDanger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val target = targetAmount.toLongOrNull()
                        when {
                            name.isBlank() -> errorMsg = "Nama kegiatan wajib diisi."
                            target == null || target <= 0 -> errorMsg = "Target nominal harus lebih dari 0."
                            startDateStr.isBlank() -> errorMsg = "Tanggal mulai wajib diisi (dd/MM/yyyy)."
                            deadlineDateStr.isBlank() -> errorMsg = "Tenggat wajib diisi (dd/MM/yyyy)."
                            else -> {
                                val startMs = parseDate(startDateStr)
                                val deadlineMs = parseDate(deadlineDateStr)
                                if (startMs == 0L || deadlineMs == 0L) {
                                    errorMsg = "Format tanggal tidak valid (gunakan dd/MM/yyyy)."
                                    return@Button
                                }
                                if (deadlineMs < startMs) {
                                    errorMsg = "Tenggat tidak boleh sebelum tanggal mulai."
                                    return@Button
                                }
                                errorMsg = null
                                coroutineScope.launch {
                                    isSaving = true
                                    val activity = existingActivity?.copy(
                                        name = name.trim(),
                                        description = description.trim(),
                                        startAtEpochMs = startMs,
                                        deadlineAtEpochMs = deadlineMs,
                                        defaultTargetAmount = target,
                                        allowLatePayment = allowLatePayment,
                                        status = status
                                    ) ?: IuranActivity(
                                        id = if (isNew) "" else (activityId ?: ""),
                                        name = name.trim(),
                                        description = description.trim(),
                                        startAtEpochMs = startMs,
                                        deadlineAtEpochMs = deadlineMs,
                                        defaultTargetAmount = target,
                                        allowLatePayment = allowLatePayment,
                                        status = status,
                                        createdBy = "ADMIN"
                                    )
                                    repository.saveActivity(activity)
                                    isSaving = false
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isNew) "Simpan Kegiatan" else "Perbarui Kegiatan", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AdminPrimary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
