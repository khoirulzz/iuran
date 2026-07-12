package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppRepository
import com.example.data.SessionStore

@Composable
fun MainApp(sessionStore: SessionStore, repository: AppRepository) {
    val navController = rememberNavController()
    val isLoggedIn = sessionStore.isLoggedIn.collectAsState(initial = false).value
    val userRole = sessionStore.userRole.collectAsState(initial = null).value

    val startDest = if (isLoggedIn) {
        if (userRole == "ADMIN") "admin_dashboard" else "officer_dashboard"
    } else {
        "login"
    }

    NavHost(navController = navController, startDestination = "splash") {

        // ==================== SPLASH ====================
        composable("splash") {
            SplashScreen(navController = navController, nextRoute = startDest)
        }

        // ==================== LOGIN ====================
        composable("login") {
            LoginScreen(navController, sessionStore, repository)
        }

        // ==================== ADMIN ====================
        composable("admin_dashboard") {
            AdminDashboardScreen(navController, sessionStore, repository)
        }

        // Form tambah/edit kegiatan: activityId = "new" untuk buat baru
        composable(
            route = "admin_activity_form/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId")
            AdminActivityFormScreen(activityId, navController, repository)
        }

        // Detail kegiatan admin (daftar peserta)
        composable(
            route = "admin_activity_detail/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            ActivityDetailScreen(activityId, navController, repository)
        }

        // Form tambah/edit warga — diakses dari FAB tab Warga admin
        composable(
            route = "admin_resident_form/{residentId}",
            arguments = listOf(navArgument("residentId") { type = NavType.StringType })
        ) { backStackEntry ->
            // ResidentFormDialog dipakai inline di AdminResidentsScreen
            // Route ini dimaksudkan untuk navigasi dalam bottom nav tab
            AdminDashboardScreen(navController, sessionStore, repository)
        }

        // Manajemen petugas
        composable("admin_officers") {
            AdminOfficersScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Semua transaksi + reversal
        composable("admin_transactions") {
            AdminTransactionsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== OFFICER ====================
        composable("officer_dashboard") {
            OfficerDashboardScreen(navController, sessionStore, repository)
        }

        // Daftar warga dalam satu kegiatan (untuk petugas)
        composable(
            route = "officer_residents/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            OfficerResidentsScreen(
                activityId = activityId,
                activityName = "",
                navController = navController,
                repository = repository
            )
        }

        // Detail satu warga dalam kegiatan (summary + riwayat + aksi bayar)
        composable(
            route = "officer_resident_detail/{activityId}/{residentId}",
            arguments = listOf(
                navArgument("activityId") { type = NavType.StringType },
                navArgument("residentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            val residentId = backStackEntry.arguments?.getString("residentId") ?: ""
            OfficerResidentDetailScreen(activityId, residentId, navController, repository)
        }

        // Form tambah pembayaran
        composable(
            route = "payment/{activityId}/{residentId}",
            arguments = listOf(
                navArgument("activityId") { type = NavType.StringType },
                navArgument("residentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            val residentId = backStackEntry.arguments?.getString("residentId") ?: ""
            PaymentScreen(activityId, residentId, navController, sessionStore, repository)
        }

        // Route lama — tetap dipelihara untuk kompatibilitas
        composable(
            route = "activity_detail/{activityId}",
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            ActivityDetailScreen(activityId, navController, repository)
        }
    }
}
