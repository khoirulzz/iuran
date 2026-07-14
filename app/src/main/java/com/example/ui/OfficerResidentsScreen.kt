@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.domain.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class ResidentFilterStatus { ALL, UNPAID, PARTIAL, PAID, OVERPAID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerResidentsScreen(
    activityId: String,
    activityName: String = "",
    navController: NavController,
    repository: AppRepository,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val coroutineScope = rememberCoroutineScope()
    var summaries by remember { mutableStateOf<List<ResidentPaymentSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf(ResidentFilterStatus.ALL) }
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(activityId) {
        isLoading = true
        repository.ensureAllResidentsEnrolled(activityId)
        val participants = repository.getParticipants(activityId)
        val list = mutableListOf<ResidentPaymentSummary>()
        for (p in participants) {
            repository.getResidentSummary(activityId, p)?.let { list.add(it) }
        }
        summaries = list
        isLoading = false
    }

    val filtered = summaries.filter { s ->
        val q = searchQuery.lowercase()
        val matchSearch = q.isEmpty() ||
            s.resident.name.lowercase().contains(q) ||
            s.resident.houseNumber.lowercase().contains(q) ||
            s.resident.block.lowercase().contains(q)
        val matchFilter = when (filterStatus) {
            ResidentFilterStatus.ALL -> true
            ResidentFilterStatus.UNPAID -> s.paymentStatus == PaymentStatus.UNPAID
            ResidentFilterStatus.PARTIAL -> s.paymentStatus == PaymentStatus.PARTIAL
            ResidentFilterStatus.PAID -> s.paymentStatus == PaymentStatus.PAID
            ResidentFilterStatus.OVERPAID -> s.paymentStatus == PaymentStatus.OVERPAID
        }
        matchSearch && matchFilter
    }.sortedBy { it.resident.name.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daftar Warga", fontWeight = FontWeight.Bold, color = Color.White)
                        if (activityName.isNotEmpty()) {
                            Text(activityName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OfficerPrimary)
            )
        }
    ) { topPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(topPadding)
        ) {
            // Search bar sticky
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama, nomor rumah, blok…") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = OfficerPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OfficerPrimary,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    // Filter chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            ResidentFilterStatus.ALL to "Semua",
                            ResidentFilterStatus.UNPAID to "Belum Bayar",
                            ResidentFilterStatus.PARTIAL to "Mencicil",
                            ResidentFilterStatus.PAID to "Lunas"
                        ).forEach { (f, label) ->
                            FilterChip(
                                selected = filterStatus == f,
                                onClick = { filterStatus = f },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OfficerLight,
                                    selectedLabelColor = OfficerPrimary
                                )
                            )
                        }
                    }
                }
                Divider(color = BorderColor)
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OfficerPrimary)
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isEmpty()) "Tidak ada warga dengan status ini"
                            else "Warga tidak ditemukan",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "${filtered.size} warga",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(filtered) { summary ->
                    OfficerResidentCard(
                        summary = summary,
                        fmt = fmt,
                        onClick = {
                            navController.navigate("officer_resident_detail/${activityId}/${summary.resident.id}")
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun OfficerResidentCard(
    summary: ResidentPaymentSummary,
    fmt: NumberFormat,
    onClick: () -> Unit
) {
    val (statusLabel, statusBg, statusFg) = when (summary.paymentStatus) {
        PaymentStatus.PAID     -> Triple("Lunas",       Color(0xFFDCFCE7), StatusPaid)
        PaymentStatus.PARTIAL  -> Triple("Mencicil",    Color(0xFFFFF3CD), StatusPartial)
        PaymentStatus.UNPAID   -> Triple("Belum Bayar", Color(0xFFFEE2E2), StatusUnpaid)
        PaymentStatus.OVERPAID -> Triple("Lebih Bayar", Color(0xFFF5F3FF), StatusOverpaid)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(OfficerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        summary.resident.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OfficerPrimary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        summary.resident.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        buildString {
                            if (summary.resident.houseNumber.isNotEmpty()) append("No. ${summary.resident.houseNumber}")
                            if (summary.resident.block.isNotEmpty()) append(" · ${summary.resident.block}")
                        }.ifEmpty { "Alamat belum diisi" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusFg)
                }
            }
            Spacer(Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { summary.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = statusFg,
                trackColor = statusBg
            )
            Spacer(Modifier.height(6.dp))

            // Angka
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Terbayar: Rp ${fmt.format(summary.totalPaid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    "Target: Rp ${fmt.format(summary.participant.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
