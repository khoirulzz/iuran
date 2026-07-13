package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.data.SessionStore
import com.example.domain.PaymentMethod
import com.example.domain.Resident
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    activityId: String,
    residentId: String,
    navController: NavController,
    sessionStore: SessionStore,
    repository: AppRepository
) {
    var resident by remember { mutableStateOf<Resident?>(null) }
    var remainingAmount by remember { mutableLongStateOf(0L) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var loading by remember { mutableStateOf(false) }
    var officerId by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var showOverpayConfirm by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(Unit) {
        resident = repository.getResident(residentId)
        sessionStore.userId.collect { id -> if (id != null) officerId = id }
    }

    LaunchedEffect(activityId, residentId) {
        val participant = repository.getParticipants(activityId).firstOrNull { it.residentId == residentId }
        val paid = repository.getResidentTotalPaid(activityId, residentId)
        remainingAmount = (participant?.targetAmount ?: 0L) - paid
    }

    fun doSavePayment() {
        val amt = amountText.replace(".", "").replace(",", "").toLongOrNull()
        if (amt == null || amt <= 0) { errorMsg = "Nominal harus lebih dari 0."; return }
        if (officerId.isEmpty()) { errorMsg = "Sesi tidak valid."; return }
        coroutineScope.launch {
            loading = true
            errorMsg = null
            val result = repository.createPayment(
                activityId = activityId,
                residentId = residentId,
                officerId = officerId,
                amount = amt,
                method = selectedMethod,
                note = note.trim()
            )
            loading = false
            if (result.isSuccess) {
                successMsg = "Pembayaran Rp ${fmt.format(amt)} berhasil disimpan."
                navController.popBackStack()
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Pembayaran gagal."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tambah Pembayaran", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            resident?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info warga & sisa tagihan
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = OfficerContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = OfficerPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                resident?.name ?: "Memuat…",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OfficerDark
                            )
                        }
                        if (remainingAmount > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Sisa tagihan: Rp ${fmt.format(remainingAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OfficerPrimary
                            )
                        } else if (remainingAmount == 0L) {
                            Spacer(Modifier.height(8.dp))
                            Text("✓ Sudah Lunas", style = MaterialTheme.typography.bodyMedium, color = OfficerPrimary, fontWeight = FontWeight.Bold)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Kelebihan bayar: Rp ${fmt.format(-remainingAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Nominal Input
            item {
                FormSection(title = "Nominal Pembayaran") {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { new ->
                            val digits = new.filter(Char::isDigit)
                            val num = digits.toLongOrNull()
                            amountText = if (num != null && num > 0) fmt.format(num) else ""
                        },
                        label = { Text("Masukkan nominal (Rp)") },
                        leadingIcon = { Text("Rp", color = OfficerPrimary, modifier = Modifier.padding(start = 12.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OfficerPrimary
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    // Nominal cepat
                    Text(
                        "Nominal Cepat",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quickAmounts = listOf(
                            "Rp 10.000" to 10_000L,
                            "Rp 20.000" to 20_000L,
                            "Rp 50.000" to 50_000L,
                            "Rp 100.000" to 100_000L,
                            "Lunasi" to remainingAmount.coerceAtLeast(0L)
                        )
                        items(quickAmounts) { (label, amount) ->
                            if (amount > 0) {
                                val formattedAmt = fmt.format(amount)
                                ElevatedButton(
                                    onClick = { amountText = formattedAmt },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (amountText == formattedAmt) OfficerLight else AppSurface,
                                        contentColor = if (amountText == formattedAmt) OfficerPrimary else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (amountText == formattedAmt) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Metode Pembayaran
            item {
                FormSection(title = "Metode Pembayaran") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            PaymentMethod.CASH to "💵 Tunai",
                            PaymentMethod.TRANSFER to "🏦 Transfer",
                            PaymentMethod.QRIS to "📱 QRIS",
                            PaymentMethod.OTHER to "📦 Lainnya"
                        ).forEach { (method, label) ->
                            ElevatedButton(
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = if (selectedMethod == method) OfficerPrimary else AppSurface,
                                    contentColor = if (selectedMethod == method) Color.White else TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Catatan
            item {
                FormSection(title = "Catatan (Opsional)") {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        placeholder = { Text("Contoh: Bayar via titip tetangga") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OfficerPrimary)
                    )
                }
            }

            // Error
            if (errorMsg != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = AccentDanger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMsg!!, color = AccentDanger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Tombol Simpan
            item {
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull()
                        if (amt != null && amt > remainingAmount && remainingAmount > 0) {
                            showOverpayConfirm = true
                        } else {
                            doSavePayment()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !loading && amountText.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OfficerPrimary)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Pembayaran", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Konfirmasi kelebihan bayar
    if (showOverpayConfirm) {
        AlertDialog(
            onDismissRequest = { showOverpayConfirm = false },
            containerColor = AppSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold, color = AccentAmber) },
            text = {
                val amt = amountText.toLongOrNull() ?: 0L
                Text(
                    "Nominal Rp ${fmt.format(amt)} melebihi sisa tagihan Rp ${fmt.format(remainingAmount)}. " +
                    "Selisih Rp ${fmt.format(amt - remainingAmount)} akan tercatat sebagai kelebihan bayar. Lanjutkan?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showOverpayConfirm = false; doSavePayment() },
                    colors = ButtonDefaults.buttonColors(containerColor = OfficerPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Ya, Lanjutkan") }
            },
            dismissButton = {
                TextButton(onClick = { showOverpayConfirm = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}
