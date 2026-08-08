package com.delivr.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.delivr.app.ui.home.HomeRoute
import com.delivr.app.ui.scan.ScanRoute
import com.delivr.app.ui.validation.ValidationRoute

/**
 * Graphe de navigation principal. Les écrans de livraison et de liste
 * viendront s'ajouter ici au fur et à mesure du développement (voir README).
 */
@Composable
fun DelivrNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                onNouvelleTournee = { navController.navigate(Routes.SCAN) },
                onReprendreTournee = { navController.navigate(Routes.VALIDATION_RESUME) },
            )
        }
        composable(Routes.SCAN) {
            ScanRoute(
                onBack = { navController.popBackStack() },
                onContinue = { imagePath -> navController.navigate(Routes.validation(imagePath)) },
            )
        }
        composable(
            route = Routes.VALIDATION,
            arguments = listOf(navArgument(Routes.VALIDATION_ARG_IMAGE_PATH) { type = NavType.StringType }),
        ) { backStackEntry ->
            val imagePath =
                Uri.decode(backStackEntry.arguments?.getString(Routes.VALIDATION_ARG_IMAGE_PATH).orEmpty())
            ValidationRoute(
                imagePath = imagePath,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.VALIDATION_RESUME) {
            ValidationRoute(
                imagePath = null,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
