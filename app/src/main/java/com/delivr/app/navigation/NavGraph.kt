package com.delivr.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.delivr.app.ui.home.HomeScreen
import com.delivr.app.ui.scan.ScanScreen

/**
 * Graphe de navigation principal. Les écrans de scan, validation et livraison
 * viendront s'ajouter ici au fur et à mesure du développement (voir README).
 */
@Composable
fun DelivrNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNouvelleTournee = { navController.navigate(Routes.SCAN) },
                onReprendreTournee = {
                    // TODO: reprendre la tournée sauvegardée une fois Room branché
                },
                hasTourneeEnCours = false
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(onBack = { navController.popBackStack() })
        }
    }
}
