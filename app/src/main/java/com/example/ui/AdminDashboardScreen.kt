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
import com.example.data.SessionStore
import com.example.domain.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    sessionStore: SessionStore,
    repository: AppRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<IuranActivity>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        activities = repository.getActivities()
        isLoading = false
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = AppSurface) {
                listOf(
                    Triple(Icons.Default.Dashboard, "Dashboard", 0),
                    Triple(Icons.Default.Event, "Kegiatan", 1),
                    Triple(Icons.Default.People, "Warga", 2),
                    Triple(Icons.Default.Assessment, "Laporan", 3),
                    Triple(Icons.Default.Person, "Akun", 4)
                ).forEach { (icon, label, idx) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        selected = selectedTab == idx,
                        onClick = {
                            if (idx == 4) {
                                coroutineScope.launch {
                                    sessionStore.clearSession()
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                }
                            } else selectedTab = idx
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AdminPrimary,
                            selectedTextColor = AdminPrimary,
                            indicatorColor = AdminLight
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { navController.navigate("admin_activity_form/new") },
                    containerColor = AdminPrimary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Tambah Kegiatan") }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { navController.navigate("admin_resident_form/new") },
                    containerColor = AdminPrimary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Warga") }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> AdminDashboardContent(activities, isLoading, navController, paddingValues, onTabChange = { selectedTab = it })
            1 -> AdminActivitiesTab(activities, navController, paddingValues)
            2 -> AdminResidentsScreen(navController, repository, paddingValues)
            3 -> AdminReportsScreen(navController, repository, paddingValues)
            4 -> {}
        }
    }
}

@Composable
private fun AdminDashboardContent(
    activities: List<IuranActivity>,
    isLoading: Boolean,
    navController: NavController,
    paddingValues: PaddingValues,
    onTabChange: (Int) -> Unit
) {
    val activeActivities = activities.filter { it.status == ActivityStatus.ACTIVE }
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        // Header Gradient
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminPrimary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Selamat datang, Admin",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Gempala Iuran · Admin",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Ringkasan Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryMetric(
                            modifier = Modifier.weight(1f),
                            label = "Kegiatan Aktif",
                            value = "${activeActivities.size}",
                            valueColor = AdminPrimary
                        )
                        SummaryMetric(
                            modifier = Modifier.weight(1f),
                            label = "Total Kegiatan",
                            value = "${activities.size}",
                            valueColor = TextPrimary
                        )
                    }
                }
            }
        }

        // Kegiatan Aktif
        item {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kegiatan Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onTabChange(1) }) {
                    Text("Lihat Semua", color = AdminPrimary)
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminPrimary)
                }
            }
        } else if (activeActivities.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Belum ada kegiatan iuran",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("admin_activity_form/new") },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                    ) { Text("Buat Kegiatan") }
                }
            }
        } else {
            items(activeActivities.take(3)) { activity ->
                AdminActivityCard(
                    activity = activity,
                    onClick = { navController.navigate("admin_activity_detail/${activity.id}") }
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        // Menu Admin Cepat
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Menu Admin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AdminMenuIcon(Icons.Default.Event, "Kegiatan") { onTabChange(1) }
                AdminMenuIcon(Icons.Default.People, "Warga") { onTabChange(2) }
                AdminMenuIcon(Icons.Default.SupervisedUserCircle, "Petugas") {
                    navController.navigate("admin_officers")
                }
                AdminMenuIcon(Icons.Default.Assessment, "Laporan") { onTabChange(3) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdminActivitiesTab(
    activities: List<IuranActivity>,
    navController: NavController,
    paddingValues: PaddingValues
) {
    var filterStatus by remember { mutableStateOf<ActivityStatus?>(null) }
    val filtered = if (filterStatus == null) activities
    else activities.filter { it.status == filterStatus }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminPrimary)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Kegiatan Iuran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        item {
            // Filter Chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = filterStatus == ActivityStatus.ACTIVE,
                    onClick = { filterStatus = ActivityStatus.ACTIVE },
                    label = { Text("Aktif") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminLight,
                        selectedLabelColor = AdminPrimary
                    )
                )
                FilterChip(
                    selected = filterStatus == ActivityStatus.DRAFT,
                    onClick = { filterStatus = ActivityStatus.DRAFT },
                    label = { Text("Draft") }
                )
                FilterChip(
                    selected = filterStatus == ActivityStatus.COMPLETED,
                    onClick = { filterStatus = ActivityStatus.COMPLETED },
                    label = { Text("Selesai") }
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada kegiatan", color = TextSecondary)
                }
            }
        } else {
            items(filtered) { activity ->
                AdminActivityCard(
                    activity = activity,
                    onClick = { navController.navigate("admin_activity_detail/${activity.id}") }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// =================== REUSABLE COMPONENTS ===================

@Composable
fun AdminActivityCard(activity: IuranActivity, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AdminLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = AdminPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        activity.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                StatusBadge(activity.status)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AdminPrimary,
                trackColor = AdminLight
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Target: Rp ${NumberFormat.getNumberInstance(Locale("id","ID")).format(activity.defaultTargetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text("75%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AdminPrimary)
            }
        }
    }
}

@Composable
fun StatusBadge(status: ActivityStatus) {
    val (label, bg, fg) = when (status) {
        ActivityStatus.ACTIVE    -> Triple("Aktif",   Color(0xFFDCFCE7), StatusPaid)
        ActivityStatus.DRAFT     -> Triple("Draft",   Color(0xFFF1F5F9), TextSecondary)
        ActivityStatus.COMPLETED -> Triple("Selesai", Color(0xFFDBEAFE), AdminPrimary)
        ActivityStatus.EXPIRED   -> Triple("Expired", Color(0xFFFEE2E2), StatusUnpaid)
        ActivityStatus.ARCHIVED  -> Triple("Arsip",   Color(0xFFF5F3FF), AccentPurple)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun SummaryMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun AdminMenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AdminLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = AdminPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
