package com.delivr.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.ui.DelivrViewModelFactories
import com.delivr.app.ui.theme.DelivrTheme

/**
 * Point d'entrée de la destination de navigation "accueil", sur le modèle de
 * `ScanRoute`/`ValidationRoute` : c'est ici que vit le `HomeViewModel`,
 * [HomeScreen] restant une fonction pure de ses paramètres (donc
 * prévisualisable et testable sans Room).
 */
@Composable
fun HomeRoute(
    onNouvelleTournee: () -> Unit,
    onReprendreTournee: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = DelivrViewModelFactories.home),
) {
    // Relancé à chaque retour sur l'accueil, pas une seule fois pour toute la
    // vie du ViewModel : le NavHost ne compose que la destination courante,
    // donc l'accueil quitte la composition quand on va scanner et y revient
    // (nouveau LaunchedEffect(Unit)) au retour arrière — alors que le
    // ViewModel, lui, survit (il est attaché à l'entrée de pile). C'est ce
    // qui fait que le bouton « Reprendre » s'active dès le retour d'une
    // extraction réussie, sans avoir besoin d'observer la base avec un Flow.
    LaunchedEffect(Unit) { viewModel.refresh() }

    HomeScreen(
        onNouvelleTournee = onNouvelleTournee,
        onReprendreTournee = onReprendreTournee,
        hasTourneeEnCours = viewModel.hasTourneeEnCours,
        modifier = modifier,
    )
}

/**
 * Écran d'accueil : point de départ de chaque tournée.
 *
 * - "Nouvelle tournée" lance le flux de scan de la feuille de livraison.
 * - "Reprendre la tournée en cours" n'est activé que si une tournée a été
 *   sauvegardée automatiquement (Phase 5, voir [HomeRoute]/[HomeViewModel]).
 */
@Composable
fun HomeScreen(
    onNouvelleTournee: () -> Unit,
    onReprendreTournee: () -> Unit,
    hasTourneeEnCours: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNouvelleTournee,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(stringResource(R.string.home_nouvelle_tournee))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onReprendreTournee,
            enabled = hasTourneeEnCours,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(stringResource(R.string.home_reprendre_tournee))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DelivrTheme {
        HomeScreen(
            onNouvelleTournee = {},
            onReprendreTournee = {},
            hasTourneeEnCours = true,
        )
    }
}
