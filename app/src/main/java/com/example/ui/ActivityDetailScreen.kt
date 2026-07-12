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
import com.example.data.AppRepository
import com.example.domain.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    navController: NavController,
    repository: AppRepository
) {
    var activity by remember { mutableStateOf<IuranActivity?>(null) }
    var summaries by remember { mutableStateOf<List<ResidentPaymentSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(activityId) {
        isLoading = true
        activity = repository.getActivityById(activityId)
        val participants = repository.getParticipants(activityId)
        summaries = participants.mapNotNull { repository.getResidentSummary(activityId, it) }
        isLoading = false
    }

    val filtered = summaries.filter { s ->
        val q = searchQuery.lowercase()
        q.isEmpty() ||
            s.resident.name.lowercase().contains(q) ||
            s.resident.houseNumber.lowercase().contains(q) ||
            s.resident.block.lowercase().contains(q)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activity?.name ?: "Daftar Warga", fontWeight = FontWeight.Bold, color = Color.White)
                        if (activity != null) {
                            StatusBadge(activity!!.status)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPrimary)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
        ) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSurface)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama warga…") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AdminPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AdminPrimary, unfocusedBorderColor = BorderColor)
                    )
                }
                Divider(color = BorderColor)
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPrimary)
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Tidak ada warga ditemukan", color = TextSecondary)
                    }
                }
            } else {
                item {
                    Text(
                        "${filtered.size} peserta",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(filtered) { summary ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { navController.navigate("payment/$activityId/${summary.resident.id}") },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(AdminLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        summary.resident.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = AdminPrimary,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(summary.resident.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(
                                        "Target: Rp ${fmt.format(summary.participant.targetAmount)}",
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                    )
                                }
                                val (label, bg, fg) = when (summary.paymentStatus) {
                                    PaymentStatus.PAID -> Triple("Lunas", Color(0xFFDCFCE7), StatusPaid)
                                    PaymentStatus.PARTIAL -> Triple("Cicil", Color(0xFFFFF3CD), StatusPartial)
                                    PaymentStatus.UNPAID -> Triple("Belum", Color(0xFFFEE2E2), StatusUnpaid)
                                    PaymentStatus.OVERPAID -> Triple("Lebih", Color(0xFFF5F3FF), StatusOverpaid)
                                }
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { summary.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AdminPrimary,
                                trackColor = AdminLight
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
