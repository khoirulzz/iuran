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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.AppRepository
import com.example.domain.Resident
import com.example.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader

// =============== LAYAR DAFTAR WARGA (dipakai di tab Admin) ===============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResidentsScreen(
    navController: NavController,
    repository: AppRepository,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var residents by remember { mutableStateOf<List<Resident>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showInactive by remember { mutableStateOf(false) }
    var selectedBlock by remember { mutableStateOf("Semua") }
    var isLoading by remember { mutableStateOf(true) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingResident by remember { mutableStateOf<Resident?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var importResultDialog by remember { mutableStateOf<String?>(null) }
    var deletingResident by remember { mutableStateOf<Resident?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val lines = reader.readLines()
                    reader.close()
                    var countImported = 0
                    var isHeader = true
                    for (line in lines) {
                        if (line.isNotBlank()) {
                            if (isHeader && line.lowercase().contains("nama")) {
                                isHeader = false
                            } else {
                                isHeader = false
                                val cols = line.split(",")
                                val nama = cols.getOrNull(0)?.trim()?.uppercase() ?: ""
                                if (nama.isNotEmpty()) {
                                    val noRumah = cols.getOrNull(1)?.trim() ?: ""
                                    val blok = cols.getOrNull(2)?.trim() ?: ""
                                    val alamat = cols.getOrNull(3)?.trim() ?: ""
                                    val telepon = cols.getOrNull(4)?.trim() ?: ""
                                    val catatan = cols.getOrNull(5)?.trim() ?: ""
                                    repository.saveResident(
                                        Resident(
                                            name = nama,
                                            houseNumber = noRumah,
                                            block = blok,
                                            address = alamat,
                                            phone = telepon,
                                            notes = catatan
                                        )
                                    )
                                    countImported++
                                }
                            }
                        }
                    }
                    residents = repository.getResidents(activeOnly = false)
                    isImporting = false
                    importResultDialog = "Berhasil mengimpor $countImported warga dari file CSV."
                } catch (e: Exception) {
                    isImporting = false
                    importResultDialog = "Gagal mengimpor CSV: ${e.message}"
                }
            }
        }
    }

    fun loadResidents() {
        coroutineScope.launch {
            isLoading = true
            residents = repository.getResidents(activeOnly = false)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadResidents() }

    val availableBlocks = remember(residents) {
        listOf("Semua") + residents
            .map { it.block.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val filtered = residents.filter { r ->
        val q = searchQuery.lowercase()
        val matchSearch = q.isEmpty() ||
            r.name.lowercase().contains(q) ||
            r.houseNumber.lowercase().contains(q) ||
            r.block.lowercase().contains(q) ||
            r.address.lowercase().contains(q)
        val matchActive = showInactive || r.isActive
        val matchBlock = selectedBlock == "Semua" || r.block.trim().equals(selectedBlock, ignoreCase = true)
        matchSearch && matchActive && matchBlock
    }.sortedBy { it.name.uppercase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari warga (nama/blok/no. rumah)...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppSurface,
                            unfocusedContainerColor = AppSurface
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !showInactive,
                            onClick = { showInactive = false },
                            label = { Text("Aktif Saja") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminLight,
                                selectedLabelColor = AdminPrimary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = showInactive,
                            onClick = { showInactive = true },
                            label = { Text("Tampilkan Nonaktif") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminLight,
                                selectedLabelColor = AdminPrimary
                            )
                        )
                    }
                    if (availableBlocks.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(availableBlocks) { block ->
                                FilterChip(
                                    selected = selectedBlock.equals(block, ignoreCase = true),
                                    onClick = { selectedBlock = block },
                                    label = { Text(if (block == "Semua") "Semua RT/Blok" else block, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AdminLight,
                                        selectedLabelColor = AdminPrimary
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { downloadTemplateCsv(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Template CSV", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { csvLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import CSV", style = MaterialTheme.typography.labelMedium)
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
            } else if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonOff, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Tidak ada data warga ditemukan", color = TextSecondary)
                    }
                }
            } else {
                items(filtered) { resident ->
                    ResidentListItem(
                        resident = resident,
                        onEdit = {
                            editingResident = resident
                            showFormDialog = true
                        },
                        onDelete = {
                            deletingResident = resident
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Mengimpor Data Warga", fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = AdminPrimary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Sedang membaca file CSV & menyimpan data warga...")
                }
            },
            confirmButton = {}
        )
    }

    if (importResultDialog != null) {
        AlertDialog(
            onDismissRequest = { importResultDialog = null },
            title = { Text("Hasil Impor CSV", fontWeight = FontWeight.Bold) },
            text = { Text(importResultDialog!!) },
            confirmButton = {
                Button(onClick = { importResultDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)) {
                    Text("OK")
                }
            }
        )
    }

    if (deletingResident != null) {
        AlertDialog(
            onDismissRequest = { deletingResident = null },
            title = { Text("Hapus Data Warga", fontWeight = FontWeight.Bold, color = AccentDanger) },
            text = { Text("Apakah Anda yakin ingin menghapus data warga '${deletingResident!!.name.uppercase()}' secara permanen?") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = deletingResident!!
                        deletingResident = null
                        coroutineScope.launch {
                            repository.deleteResident(target.id)
                            loadResidents()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingResident = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog form tambah / edit warga
    if (showFormDialog) {
        ResidentFormDialog(
            initial = editingResident,
            onDismiss = {
                showFormDialog = false
                editingResident = null
            },
            onSave = { resident ->
                coroutineScope.launch {
                    repository.saveResident(resident)
                    showFormDialog = false
                    editingResident = null
                    loadResidents()
                }
            }
        )
    }
}

@Composable
private fun ResidentListItem(
    resident: Resident,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (resident.isActive) AppSurface else Color(0xFFF8FAFC)
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
                    .background(if (resident.isActive) AdminLight else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    resident.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (resident.isActive) AdminPrimary else TextDisabled
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        resident.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (resident.isActive) TextPrimary else TextDisabled
                    )
                    if (!resident.isActive) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Nonaktif", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                        }
                    }
                }
                Text(
                    buildString {
                        if (resident.houseNumber.isNotEmpty()) append("No. ${resident.houseNumber}")
                        if (resident.block.isNotEmpty()) append(" · ${resident.block}")
                        if (resident.address.isNotEmpty()) append(" · ${resident.address}")
                    }.ifEmpty { "Alamat belum diisi" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = AdminPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = AccentDanger, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentFormDialog(
    initial: Resident?,
    onDismiss: () -> Unit,
    onSave: (Resident) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var houseNumber by remember { mutableStateOf(initial?.houseNumber ?: "") }
    var block by remember { mutableStateOf(initial?.block ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (initial == null) "Tambah Warga" else "Edit Data Warga",
                fontWeight = FontWeight.Bold,
                color = AdminPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    isError = errorMsg != null && name.isBlank()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = houseNumber,
                        onValueChange = { houseNumber = it },
                        label = { Text("No. Rumah") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Blok") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon (opsional)") },
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
                    if (name.isBlank()) {
                        errorMsg = "Nama wajib diisi."
                    } else {
                        onSave(
                            (initial ?: Resident()).copy(
                                name = name.trim(),
                                houseNumber = houseNumber.trim(),
                                block = block.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

private fun downloadTemplateCsv(context: android.content.Context) {
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, "template_data_warga.csv")
        file.writeText("Nama Lengkap,No. Rumah,Blok,Alamat,No. Telepon,Catatan\nAhmad Subagja,12,A1,Jl. Melati No. 12,081234567890,Warga Aktif\nSiti Aminah,5,B2,Jl. Mawar No. 5,081987654321,\n")
        android.widget.Toast.makeText(context, "Template CSV berhasil disimpan ke Download/template_data_warga.csv", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal mengunduh template: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
