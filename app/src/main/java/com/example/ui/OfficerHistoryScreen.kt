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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerHistoryScreen(
    officerId: String,
    repository: AppRepository,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    var transactions by remember { mutableStateOf<List<PaymentTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    LaunchedEffect(officerId) {
        if (officerId.isNotEmpty()) {
            isLoading = true
            repository.getMyTransactionsFlow(officerId).collect { list ->
                transactions = list
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OfficerPrimary)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Riwayat Transaksi Saya",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Ringkasan harian hari ini
        item {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale("id","ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(Date())
            val todayTx = transactions.filter { it.paidDateKey == todayKey && it.type == TransactionType.PAYMENT }
            val todayTotal = todayTx.sumOf { it.amount }

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = OfficerContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Hari Ini",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OfficerDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Transaksi", style = MaterialTheme.typography.bodySmall, color = OfficerPrimary.copy(alpha = 0.7f))
                            Text(
                                "${todayTx.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = OfficerPrimary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Total Dikumpulkan", style = MaterialTheme.typography.bodySmall, color = OfficerPrimary.copy(alpha = 0.7f))
                            Text(
                                "Rp ${fmt.format(todayTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OfficerPrimary
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OfficerPrimary)
                }
            }
        } else if (transactions.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ReceiptLong, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada transaksi", color = TextSecondary)
                }
            }
        } else {
            item {
                Text(
                    "Semua Riwayat (${transactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(transactions) { tx ->
                HistoryTransactionItem(tx = tx, fmt = fmt, sdf = sdf)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HistoryTransactionItem(
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isReversal) Color(0xFFFEE2E2) else OfficerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isReversal) Icons.Default.Undo else Icons.Default.Payments,
                    null,
                    tint = if (isReversal) AccentDanger else OfficerPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReversal) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("REVERSAL", style = MaterialTheme.typography.labelSmall, color = AccentDanger, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        methodLabel(tx.paymentMethod),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isReversal) AccentDanger else TextPrimary
                    )
                }
                Text(
                    sdf.format(Date(tx.paidAtDeviceEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (tx.note.isNotEmpty()) {
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
                }
                // Sync status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDone,
                        null,
                        tint = OfficerPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tersinkron", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
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
