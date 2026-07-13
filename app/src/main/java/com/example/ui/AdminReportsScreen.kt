package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.domain.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    navController: androidx.navigation.NavController,
    repository: AppRepository,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val coroutineScope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<IuranActivity>>(emptyList()) }
    var summaries by remember { mutableStateOf<List<ActivitySummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedActivityId by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf<List<PaymentTransaction>>(emptyList()) }

    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(Unit) {
        isLoading = true
        val acts = repository.getActivities()
        activities = acts.filter { it.status == ActivityStatus.ACTIVE || it.status == ActivityStatus.COMPLETED }
        val sumList = mutableListOf<ActivitySummary>()
        for (a in activities) {
            sumList.add(repository.getActivitySummary(a))
        }
        summaries = sumList
        isLoading = false
    }

    LaunchedEffect(selectedActivityId) {
        if (selectedActivityId != null) {
            transactions = repository.getAllTransactions(selectedActivityId)
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
                    .background(AdminPrimary)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Laporan & Rekapan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminPrimary)
                }
            }
        } else if (summaries.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Assessment, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada data laporan", color = TextSecondary)
                }
            }
        } else {
            // Rekap keseluruhan
            item {
                val totalTarget = summaries.sumOf { it.totalTarget }
                val totalCollected = summaries.sumOf { it.totalCollected }
                val totalPaid = summaries.sumOf { it.countPaid }
                val totalPartial = summaries.sumOf { it.countPartial }
                val totalUnpaid = summaries.sumOf { it.countUnpaid }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Ringkasan Seluruh Kegiatan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AdminPrimary
                        )
                        Spacer(Modifier.height(16.dp))

                        // Progress Bar Total
                        val fraction = if (totalTarget == 0L) 0f else (totalCollected.toFloat() / totalTarget).coerceIn(0f, 1f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = AdminPrimary,
                                trackColor = AdminLight
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("${(fraction * 100).toInt()}%", fontWeight = FontWeight.Bold, color = AdminPrimary)
                        }
                        Spacer(Modifier.height(16.dp))

                        // Grid 2x2
                        Row(Modifier.fillMaxWidth()) {
                            ReportMetricItem(
                                modifier = Modifier.weight(1f),
                                label = "Total Terkumpul",
                                value = "Rp ${fmt.format(totalCollected)}",
                                valueColor = OfficerPrimary
                            )
                            ReportMetricItem(
                                modifier = Modifier.weight(1f),
                                label = "Total Target",
                                value = "Rp ${fmt.format(totalTarget)}",
                                valueColor = TextPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = BorderColor)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            ReportMetricItem(Modifier.weight(1f), "Lunas", "$totalPaid warga", OfficerPrimary)
                            ReportMetricItem(Modifier.weight(1f), "Mencicil", "$totalPartial warga", AccentAmber)
                            ReportMetricItem(Modifier.weight(1f), "Belum Bayar", "$totalUnpaid warga", AccentDanger)
                        }
                    }
                }
            }

            // Per kegiatan
            item {
                Text(
                    "Rincian Per Kegiatan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(summaries) { summary ->
                ActivityReportCard(
                    summary = summary,
                    isSelected = selectedActivityId == summary.activity.id,
                    onClick = {
                        selectedActivityId = if (selectedActivityId == summary.activity.id) null
                        else summary.activity.id
                    },
                    transactions = if (selectedActivityId == summary.activity.id) transactions else emptyList()
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ActivityReportCard(
    summary: ActivitySummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    transactions: List<PaymentTransaction>
) {
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) AdminLight else AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        summary.activity.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Rp ${fmt.format(summary.totalCollected)} / Rp ${fmt.format(summary.totalTarget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    "${summary.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdminPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { summary.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AdminPrimary,
                trackColor = AdminLight
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusCount("✓ Lunas", summary.countPaid, OfficerPrimary)
                StatusCount("◑ Cicil", summary.countPartial, AccentAmber)
                StatusCount("✗ Belum", summary.countUnpaid, AccentDanger)
                if (summary.countOverpaid > 0)
                    StatusCount("↑ Lebih", summary.countOverpaid, AccentPurple)
            }

            // Expanded: breakdown per metode pembayaran
            if (isSelected && transactions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = BorderColor)
                Spacer(Modifier.height(12.dp))
                Text("Breakdown Metode Pembayaran", style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val payTx = transactions.filter { it.type == TransactionType.PAYMENT }
                val byCash = payTx.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
                val byTransfer = payTx.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.amount }
                val byQris = payTx.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.amount }
                val byOther = payTx.filter { it.paymentMethod == PaymentMethod.OTHER }.sumOf { it.amount }
                val reversals = transactions.filter { it.type == TransactionType.REVERSAL }.sumOf { it.amount }

                listOf(
                    Triple("💵 Tunai", byCash, AccentInfo),
                    Triple("🏦 Transfer", byTransfer, AdminPrimary),
                    Triple("📱 QRIS", byQris, AccentPurple),
                    Triple("📦 Lainnya", byOther, AccentAmber),
                    Triple("↩ Reversal", reversals, AccentDanger)
                ).filter { it.second != 0L }.forEach { (label, amount, color) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text(
                            "Rp ${fmt.format(amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = { exportActivityReportToPdf(context, summary, transactions) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp), tint = AdminPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Unduh Laporan PDF", style = MaterialTheme.typography.labelMedium, color = AdminPrimary)
                }
            }
        }
    }
}

private fun exportActivityReportToPdf(
    context: android.content.Context,
    summary: ActivitySummary,
    transactions: List<PaymentTransaction>
) {
    try {
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint()
        val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

        titlePaint.color = android.graphics.Color.rgb(30, 41, 59)
        titlePaint.textSize = 16f
        titlePaint.isFakeBoldText = true

        paint.color = android.graphics.Color.rgb(51, 65, 85)
        paint.textSize = 11f

        var yPos = 45f
        canvas.drawText("LAPORAN KEGIATAN IURAN GEMPALA", 40f, yPos, titlePaint)
        yPos += 25f

        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Kegiatan: ${summary.activity.name}", 40f, yPos, paint)
        yPos += 20f

        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Total Terkumpul: Rp ${fmt.format(summary.totalCollected)} dari Target Rp ${fmt.format(summary.totalTarget)} (${summary.progressPercent}%)", 40f, yPos, paint)
        yPos += 18f
        canvas.drawText("Lunas: ${summary.countPaid} Warga | Mencicil: ${summary.countPartial} Warga | Belum Bayar: ${summary.countUnpaid} Warga", 40f, yPos, paint)
        yPos += 30f

        paint.isFakeBoldText = true
        canvas.drawText("Kode Transaksi", 40f, yPos, paint)
        canvas.drawText("Nominal", 250f, yPos, paint)
        canvas.drawText("Metode", 370f, yPos, paint)
        canvas.drawText("Tanggal", 460f, yPos, paint)
        yPos += 16f
        paint.isFakeBoldText = false

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val listToDraw = if (transactions.isNotEmpty()) transactions.take(35) else emptyList()
        if (listToDraw.isEmpty()) {
            canvas.drawText("- Belum ada rincian transaksi tersimpan -", 40f, yPos, paint)
        } else {
            for (tx in listToDraw) {
                if (yPos > 780f) break
                val txLabel = if (tx.transactionId.length > 20) tx.transactionId.take(20) + "..." else tx.transactionId
                canvas.drawText(txLabel, 40f, yPos, paint)
                canvas.drawText("Rp ${fmt.format(tx.amount)}", 250f, yPos, paint)
                canvas.drawText(tx.paymentMethod.name, 370f, yPos, paint)
                canvas.drawText(sdf.format(java.util.Date(tx.paidAtDeviceEpochMs)), 460f, yPos, paint)
                yPos += 16f
            }
        }

        pdfDoc.finishPage(page)

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val fileName = "Laporan_Iuran_${summary.activity.name.replace(Regex("[^A-Za-z0-9]"), "_")}.pdf"
        val file = java.io.File(downloadsDir, fileName)
        val out = java.io.FileOutputStream(file)
        pdfDoc.writeTo(out)
        pdfDoc.close()
        out.close()

        android.widget.Toast.makeText(context, "Laporan PDF berhasil diunduh ke folder Download: ${fileName}", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal mengunduh PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun ReportMetricItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun StatusCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
