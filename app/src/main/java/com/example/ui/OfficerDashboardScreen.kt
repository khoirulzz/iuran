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
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.data.AppRepository
import com.example.data.SessionStore
import com.example.domain.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerDashboardScreen(
    navController: NavController,
    sessionStore: SessionStore,
    repository: AppRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<IuranActivity>>(emptyList()) }
    var officerName by remember { mutableStateOf("") }
    var officerId by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        sessionStore.userName.collect { name -> name?.let { officerName = it } }
    }

    LaunchedEffect(Unit) {
        sessionStore.userId.collect { id ->
            if (id != null) {
                officerId = id
                isLoading = true
                repository.getActiveActivitiesForOfficerFlow(id).collect { list ->
                    activities = list
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = AppSurface) {
                listOf(
                    Triple(Icons.Default.Home, "Beranda", 0),
                    Triple(Icons.Default.People, "Warga", 1),
                    Triple(Icons.Default.History, "Riwayat", 2),
                    Triple(Icons.Default.Person, "Akun", 3)
                ).forEach { (icon, label, idx) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        selected = selectedTab == idx,
                        onClick = {
                            selectedTab = idx
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OfficerPrimary,
                            selectedTextColor = OfficerPrimary,
                            indicatorColor = OfficerLight
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> OfficerHomeContent(
                officerName = officerName,
                officerId = officerId,
                activities = activities,
                isLoading = isLoading,
                navController = navController,
                paddingValues = paddingValues,
                repository = repository
            )
            1 -> OfficerResidentsTab(officerId, activities, navController, repository, paddingValues)
            2 -> OfficerHistoryScreen(officerId, repository, paddingValues)
            3 -> AccountScreen(
                primaryColor = OfficerPrimary,
                paddingValues = paddingValues,
                onLogout = {
                    coroutineScope.launch {
                        sessionStore.clearSession()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
    }
}

@Composable
private fun OfficerHomeContent(
    officerName: String,
    officerId: String,
    activities: List<IuranActivity>,
    isLoading: Boolean,
    navController: NavController,
    paddingValues: PaddingValues,
    repository: AppRepository
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(paddingValues)
    ) {
        // Header Hijau
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OfficerPrimary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Halo, ${officerName.ifEmpty { "Petugas" }} 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Petugas · Gempala Iuran",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(Icons.Default.Notifications, null, tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Banner Sinkronisasi
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Semua data tersinkron",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OfficerPrimary
                        )
                        Text(
                            "Firestore offline persistence aktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(Icons.Default.CloudDone, null, tint = OfficerPrimary, modifier = Modifier.size(28.dp))
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
                TextButton(onClick = {}) {
                    Text("Lihat Semua", color = OfficerPrimary)
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OfficerPrimary)
                }
            }
        } else if (activities.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EventBusy, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada kegiatan aktif yang ditugaskan", color = TextSecondary)
                }
            }
        } else {
            items(activities) { activity ->
                OfficerActivityCard(
                    activity = activity,
                    onClick = { navController.navigate("officer_residents/${activity.id}") }
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        // Menu Cepat
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Menu Cepat",
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
                OfficerMenuIcon(Icons.Default.People, "Daftar Warga") {
                    if (activities.isNotEmpty()) navController.navigate("officer_residents/${activities.first().id}")
                }
                OfficerMenuIcon(Icons.Default.AddCircle, "Input Pembayaran") {
                    if (activities.isNotEmpty()) navController.navigate("officer_residents/${activities.first().id}")
                }
                OfficerMenuIcon(Icons.Default.History, "Riwayat") {
                    // switch to history tab handled in parent
                }
                OfficerMenuIcon(Icons.Default.CloudSync, "Sinkronisasi") {
                    // Manual sync: Firestore SDK handles auto-sync
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Motivasi banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OfficerContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ayo selesaikan penarikan iuran dengan semangat! 💪",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = OfficerDark,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.EmojiPeople, null, tint = OfficerPrimary, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun OfficerResidentsTab(
    officerId: String,
    activities: List<IuranActivity>,
    navController: NavController,
    repository: AppRepository,
    paddingValues: PaddingValues
) {
    if (activities.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(AppBackground).padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EventBusy, null, tint = TextDisabled, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pilih kegiatan terlebih dahulu", color = TextSecondary)
            }
        }
    } else {
        // Default tampilkan warga kegiatan pertama
        val firstActivity = activities.first()
        OfficerResidentsScreen(
            activityId = firstActivity.id,
            activityName = firstActivity.name,
            navController = navController,
            repository = repository,
            paddingValues = paddingValues
        )
    }
}

@Composable
fun OfficerActivityCard(activity: IuranActivity, onClick: () -> Unit) {
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
                        .background(OfficerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Event, null, tint = OfficerPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        activity.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Target: Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id","ID")).format(activity.defaultTargetAmount)} / warga",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = OfficerPrimary,
                trackColor = OfficerLight
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Tekan untuk buka penarikan",
                    style = MaterialTheme.typography.labelSmall,
                    color = OfficerPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text("75%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = OfficerPrimary)
            }
        }
    }
}

@Composable
fun OfficerMenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OfficerLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = OfficerPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
