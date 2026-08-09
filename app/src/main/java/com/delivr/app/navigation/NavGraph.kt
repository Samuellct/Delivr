package com.delivr.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.delivr.app.ui.delivery.DeliveryRoute
import com.delivr.app.ui.home.HomeRoute
import com.delivr.app.ui.scan.ScanRoute
import com.delivr.app.ui.validation.ValidationRoute

/**
 * Graphe de navigation principal. L'écran de liste (Phase 7) viendra
 * s'ajouter ici au fur et à mesure du développement (voir README).
 */
@Composable
fun DelivrNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                onNouvelleTournee = { navController.navigate(Routes.SCAN) },
                // Depuis l'accueil, la pile est simplement [Home] : rien à
                // dépiler, donc pas de popUpTo. launchSingleTop évite qu'un
                // double tap n'empile deux fois le mode Livraison.
                onReprendreTournee = {
                    navController.navigate(Routes.DELIVERY) { launchSingleTop = true }
                },
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
                // Scan et Validation sont dépilés : une fois la tournée
                // démarrée, le retour système depuis le mode Livraison doit
                // ramener à l'accueil, pas rouvrir un scan terminé (dont le
                // fichier image peut d'ailleurs avoir disparu). Pile
                // résultante : [Home, Delivery].
                onStartDelivery = {
                    navController.navigate(Routes.DELIVERY) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.DELIVERY) {
            DeliveryRoute(onBackToHome = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
    }
}
