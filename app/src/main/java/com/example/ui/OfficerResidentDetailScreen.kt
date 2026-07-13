package com.example.ui

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerResidentDetailScreen(
    activityId: String,
    residentId: String,
    navController: NavController,
    repository: AppRepository
) {
    var summary by remember { mutableStateOf<ResidentPaymentSummary?>(null) }
    var transactions by remember { mutableStateOf<List<PaymentTransaction>>(emptyList()) }
    var activityName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    LaunchedEffect(activityId, residentId) {
        isLoading = true
        val activity = repository.getActivityById(activityId)
        activityName = activity?.name ?: ""

        val participant = repository.getParticipants(activityId)
            .firstOrNull { it.residentId == residentId }

        transactions = repository.getTransactions(activityId, residentId)
        if (participant != null) {
            summary = repository.getResidentSummary(activityId, participant)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            summary?.resident?.name ?: "Detail Warga",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OfficerPrimary),
                actions = {
                    IconButton(onClick = {
                        navController.navigate("payment/$activityId/$residentId")
                    }) {
                        Icon(Icons.Default.AddCircle, "Tambah Pembayaran", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("payment/$activityId/$residentId") },
                containerColor = OfficerPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Tambah Pembayaran", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues).background(AppBackground),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = OfficerPrimary) }
        } else if (summary == null) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues).background(AppBackground),
                contentAlignment = Alignment.Center
            ) { Text("Data warga tidak ditemukan", color = TextSecondary) }
        } else {
            val s = summary!!
            val (_, statusBg, statusFg) = when (s.paymentStatus) {
                PaymentStatus.PAID     -> Triple("Lunas",       Color(0xFFDCFCE7), StatusPaid)
                PaymentStatus.PARTIAL  -> Triple("Mencicil",    Color(0xFFFFF3CD), StatusPartial)
                PaymentStatus.UNPAID   -> Triple("Belum Bayar", Color(0xFFFEE2E2), StatusUnpaid)
                PaymentStatus.OVERPAID -> Triple("Lebih Bayar", Color(0xFFF5F3FF), StatusOverpaid)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(paddingValues)
            ) {
                // Info warga
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(OfficerLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        s.resident.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OfficerPrimary
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        s.resident.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        buildString {
                                            if (s.resident.houseNumber.isNotEmpty()) append("No. ${s.resident.houseNumber}")
                                            if (s.resident.block.isNotEmpty()) append(" · ${s.resident.block}")
                                        }.ifEmpty { "Alamat belum diisi" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (s.resident.phone.isNotEmpty()) {
                                        Text(s.resident.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Pembayaran
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = statusBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Target", style = MaterialTheme.typography.bodySmall, color = statusFg.copy(alpha = 0.7f))
                                    Text(
                                        "Rp ${fmt.format(s.participant.targetAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("Terbayar", style = MaterialTheme.typography.bodySmall, color = statusFg.copy(alpha = 0.7f))
                                    Text(
                                        "Rp ${fmt.format(s.totalPaid)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { s.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = statusFg,
                                trackColor = statusFg.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val sisa = s.participant.targetAmount - s.totalPaid
                                Text(
                                    when {
                                        sisa > 0 -> "Sisa: Rp ${fmt.format(sisa)}"
                                        sisa < 0 -> "Kelebihan: Rp ${fmt.format(-sisa)}"
                                        else -> "Lunas ✓"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusFg
                                )
                                Text(
                                    "${(s.progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg
                                )
                            }
                        }
                    }
                }

                // Riwayat Transaksi
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Riwayat Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${transactions.size} transaksi",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada pembayaran", color = TextSecondary)
                        }
                    }
                } else {
                    items(transactions) { tx ->
                        ResidentTransactionItem(tx, fmt, sdf)
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun ResidentTransactionItem(
    tx: PaymentTransaction,
    fmt: NumberFormat,
    sdf: SimpleDateFormat
) {
    val isReversal = tx.type == TransactionType.REVERSAL
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReversal) Color(0xFFFFF7F7) else AppSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (isReversal) Color(0xFFFEE2E2) else OfficerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isReversal) Icons.Default.Undo else Icons.Default.Payments,
                    null,
                    tint = if (isReversal) AccentDanger else OfficerPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isReversal) "REVERSAL · ${methodLabel(tx.paymentMethod)}" else methodLabel(tx.paymentMethod),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isReversal) AccentDanger else TextPrimary
                )
                Text(
                    sdf.format(Date(tx.paidAtDeviceEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (tx.note.isNotEmpty()) {
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
                }
            }
            Text(
                "${if (isReversal) "-" else "+"}Rp ${fmt.format(Math.abs(tx.amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isReversal) AccentDanger else OfficerPrimary
            )
        }
    }
}
