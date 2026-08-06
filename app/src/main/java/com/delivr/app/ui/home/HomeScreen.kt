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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.delivr.app.R
import com.delivr.app.ui.theme.DelivrTheme

/**
 * Écran d'accueil : point de départ de chaque tournée.
 *
 * - "Nouvelle tournée" lance le flux de scan de la feuille de livraison.
 * - "Reprendre la tournée en cours" n'est activé que si une tournée a été
 *   sauvegardée automatiquement (branché sur Room dans une itération future).
 */
@Composable
fun HomeScreen(
    onNouvelleTournee: () -> Unit,
    onReprendreTournee: () -> Unit,
    hasTourneeEnCours: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNouvelleTournee,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.home_nouvelle_tournee))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onReprendreTournee,
            enabled = hasTourneeEnCours,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
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
            hasTourneeEnCours = true
        )
    }
}
