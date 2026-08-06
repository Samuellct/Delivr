package com.delivr.app.ui.scan

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.camera.rememberDocumentScannerLauncher
import com.delivr.app.utils.rememberBitmapFromUri

/**
 * Écran de scan : capture de la feuille de livraison via le scanner de
 * documents ML Kit (détection des bords, redressement de perspective et
 * amélioration du contraste sont gérés par le SDK, voir
 * [com.delivr.app.camera.rememberDocumentScannerLauncher]).
 *
 * Le scan démarre automatiquement à l'arrivée sur l'écran pour limiter les
 * manipulations ; l'utilisateur peut relancer manuellement en cas
 * d'annulation ou d'erreur.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val startScan = rememberDocumentScannerLauncher(onResult = viewModel::onScanOutcome)

    LaunchedEffect(Unit) {
        viewModel.onScanStarted()
        startScan()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ScanUiState.Idle, ScanUiState.Scanning -> ScanLoading()

            is ScanUiState.Success -> ScanSuccess(
                imageUri = state.imageUri,
                onRescan = {
                    viewModel.onScanStarted()
                    startScan()
                },
                onBack = onBack
            )

            ScanUiState.Cancelled -> ScanMessage(
                title = "Scan annulé",
                message = "Tu peux relancer le scan ou revenir à l'accueil.",
                onRetry = {
                    viewModel.onScanStarted()
                    startScan()
                },
                onBack = onBack
            )

            is ScanUiState.Error -> ScanMessage(
                title = "Échec du scan",
                message = state.message,
                onRetry = {
                    viewModel.onScanStarted()
                    startScan()
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun ScanLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Ouverture du scanner…")
    }
}

@Composable
private fun ScanSuccess(
    imageUri: Uri,
    onRescan: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBitmapFromUri(imageUri)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Document scanné", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Aperçu du document scanné",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRescan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Recommencer")
        }

        Spacer(Modifier.height(8.dp))

        // Le passage à l'extraction OCR (colonne "Cott") est une étape
        // ultérieure de la feuille de route (voir TODO_V1.md, étape 2).
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continuer (extraction à venir)")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour à l'accueil")
        }
    }
}

@Composable
private fun ScanMessage(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Scanner la feuille")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("Retour à l'accueil")
        }
    }
}
