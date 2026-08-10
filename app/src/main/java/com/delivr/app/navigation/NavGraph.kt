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
import com.delivr.app.ui.list.ListRoute
import com.delivr.app.ui.scan.ScanRoute
import com.delivr.app.ui.validation.ValidationRoute

/**
 * Graphe de navigation principal.
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
                    navController.navigate(Routes.delivery()) { launchSingleTop = true }
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
                    navController.navigate(Routes.delivery()) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = Routes.DELIVERY,
            arguments =
                listOf(
                    navArgument(Routes.DELIVERY_ARG_COTTAGE_NUMBER) {
                        type = NavType.IntType
                        defaultValue = Routes.DELIVERY_NO_COTTAGE
                    },
                ),
        ) { backStackEntry ->
            val cottageNumber =
                backStackEntry.arguments?.getInt(Routes.DELIVERY_ARG_COTTAGE_NUMBER) ?: Routes.DELIVERY_NO_COTTAGE
            DeliveryRoute(
                onBackToHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onListRequested = { navController.navigate(Routes.LIST) { launchSingleTop = true } },
                focusOnCottageNumber = cottageNumber.takeIf { it != Routes.DELIVERY_NO_COTTAGE },
            )
        }
        composable(Routes.LIST) {
            ListRoute(
                onBack = { navController.popBackStack() },
                // Pas de popUpTo ici, volontairement : le retour système
                // depuis ce cottage ciblé doit ramener à la Liste (pour
                // enchaîner sur un autre cottage), pas à l'accueil.
                onCottageSelected = { number ->
                    navController.navigate(Routes.delivery(number)) { launchSingleTop = true }
                },
            )
        }
    }
}
