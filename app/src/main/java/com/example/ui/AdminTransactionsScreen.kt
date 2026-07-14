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
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.domain.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionsScreen(
    repository: AppRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf<List<PaymentTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTx by remember { mutableStateOf<PaymentTransaction?>(null) }
    var showReversalDialog by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

    fun loadTransactions() {
        coroutineScope.launch {
            isLoading = true
            transactions = repository.getAllTransactions()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadTransactions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Semua Transaksi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
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
        ) {
            if (successMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        "${transactions.size} transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(transactions) { tx ->
                    TransactionListItem(
                        tx = tx,
                        fmt = fmt,
                        sdf = sdf,
                        onCreateReversal = if (tx.type == TransactionType.PAYMENT) ({
                            selectedTx = tx
                            showReversalDialog = true
                        }) else null,
                        onEdit = if (tx.type == TransactionType.PAYMENT) ({
                            editingTx = tx
                        }) else null
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showReversalDialog && selectedTx != null) {
        ReversalDialog(
            tx = selectedTx!!,
            fmt = fmt,
            onDismiss = { showReversalDialog = false; selectedTx = null },
            onConfirm = { reason ->
                coroutineScope.launch {
                    repository.createReversal(
                        originalTransactionId = selectedTx!!.transactionId,
                        originalTransaction = selectedTx!!,
                        reason = reason,
                        adminId = "admin"
                    )
                    successMsg = "Reversal berhasil dibuat."
                    showReversalDialog = false
                    selectedTx = null
                    loadTransactions()
                }
            }
        )
    }

    var editingTx by remember { mutableStateOf<PaymentTransaction?>(null) }
    if (editingTx != null) {
        EditTransactionDialog(
            tx = editingTx!!,
            fmt = fmt,
            onDismiss = { editingTx = null },
            onSave = { newAmount, newMethod, newNote ->
                coroutineScope.launch {
                    repository.editTransaction(editingTx!!.transactionId, newAmount, newMethod, newNote)
                    successMsg = "Transaksi berhasil diperbarui."
                    editingTx = null
                    loadTransactions()
                }
            }
        )
    }
}

@Composable
fun TransactionListItem(
    tx: PaymentTransaction,
    fmt: NumberFormat,
    sdf: SimpleDateFormat,
    onCreateReversal: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val isReversal = tx.type == TransactionType.REVERSAL
    val amountColor = if (isReversal) AccentDanger else OfficerPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (isReversal) Color(0xFFFEE2E2) else AdminLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReversal) Icons.Default.Undo else Icons.Default.Receipt,
                    contentDescription = null,
                    tint = if (isReversal) AccentDanger else AdminPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.transactionId,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    methodLabel(tx.paymentMethod),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    sdf.format(Date(tx.paidAtDeviceEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isReversal) "-" else "+"}Rp ${fmt.format(Math.abs(tx.amount))}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Row {
                    if (onEdit != null) {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Edit", style = MaterialTheme.typography.labelSmall, color = AdminPrimary)
                        }
                    }
                    if (onCreateReversal != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onCreateReversal,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Batalkan", style = MaterialTheme.typography.labelSmall, color = AccentDanger)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReversalDialog(
    tx: PaymentTransaction,
    fmt: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Batalkan Transaksi", fontWeight = FontWeight.Bold, color = AccentDanger) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Transaksi yang akan dibatalkan:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "Rp ${fmt.format(tx.amount)} · ${methodLabel(tx.paymentMethod)}",
                            fontWeight = FontWeight.Bold,
                            color = AccentDanger
                        )
                        Text(
                            "ID: ${tx.transactionId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    "⚠️ Transaksi asli tidak akan dihapus. Sistem akan membuat transaksi REVERSAL bernilai negatif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan pembatalan *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                if (errorMsg != null) Text(errorMsg!!, color = AccentDanger, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) errorMsg = "Alasan wajib diisi."
                    else onConfirm(reason.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
            ) { Text("Konfirmasi Batalkan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionDialog(
    tx: PaymentTransaction,
    fmt: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (Long, PaymentMethod, String) -> Unit
) {
    var rawAmount by remember { mutableStateOf(tx.amount.toString()) }
    var selectedMethod by remember { mutableStateOf(tx.paymentMethod) }
    var note by remember { mutableStateOf(tx.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Edit Transaksi Pembayaran", fontWeight = FontWeight.Bold, color = AdminPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Perbaiki nominal atau metode pembayaran:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Baru (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Metode Pembayaran:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.values().forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(methodLabel(m)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toLongOrNull() ?: tx.amount
                    onSave(amt, selectedMethod, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

fun methodLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Tunai"
    PaymentMethod.TRANSFER -> "Transfer"
    PaymentMethod.QRIS -> "QRIS"
    PaymentMethod.OTHER -> "Lainnya"
}
