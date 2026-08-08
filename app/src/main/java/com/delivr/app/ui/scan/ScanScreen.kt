package com.delivr.app.ui.scan

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.camera.ScanError
import com.delivr.app.camera.rememberDocumentScannerLauncher
import com.delivr.app.utils.ImageLoadState
import com.delivr.app.utils.rememberBitmapFromFile

/**
 * Point d'entrée de la destination de navigation "scan" : c'est ici, et pas
 * dans [ScanScreen], qu'est instancié le lanceur ML Kit
 * ([rememberDocumentScannerLauncher]). Séparer les deux rend [ScanScreen]
 * indépendant du SDK/de Play services — testable et prévisualisable en lui
 * passant un `startScan` factice (voir `ScanScreenTest.kt`).
 */
@Composable
fun ScanRoute(
    onBack: () -> Unit,
    onContinue: (imagePath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(),
) {
    val startScan = rememberDocumentScannerLauncher(onResult = viewModel::onScanOutcome)
    ScanScreen(
        onBack = onBack,
        onContinue = onContinue,
        startScan = startScan,
        modifier = modifier,
        viewModel = viewModel,
    )
}

/**
 * Écran de scan : capture de la feuille de livraison via le scanner de
 * documents ML Kit (détection des bords, redressement de perspective et
 * amélioration du contraste sont gérés par le SDK — orchestré par
 * [rememberDocumentScannerLauncher], appelé par [ScanRoute]). Ce composable
 * ne connaît que [startScan], une fonction opaque : aucune dépendance
 * directe à ML Kit ou Play services, donc testable/prévisualisable sans eux.
 *
 * Le scan démarre automatiquement à l'arrivée sur l'écran pour limiter les
 * manipulations ; l'utilisateur peut relancer manuellement en cas
 * d'annulation ou d'erreur.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onContinue: (imagePath: String) -> Unit,
    startScan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(),
) {
    val uiState = viewModel.uiState
    val restart: () -> Unit = {
        viewModel.onScanStarted()
        startScan()
    }

    LaunchedEffect(Unit) {
        // Conditionné à Idle : après une rotation (ou une recréation de
        // process, voir ScanViewModel), uiState vaut déjà Success/Cancelled/
        // Error — jamais Idle — donc cet effet ne relance pas la caméra et ne
        // détruit pas un scan déjà terminé. Sans ce garde-fou, toute
        // recomposition initiale (rotation, changement de langue, mode
        // sombre) réexécutait cet effet inconditionnellement.
        if (viewModel.uiState is ScanUiState.Idle) {
            restart()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ScanUiState.Idle, ScanUiState.Scanning -> ScanLoading()

            is ScanUiState.Success ->
                ScanSuccess(
                    imagePath = state.imagePath,
                    onRescan = restart,
                    onContinue = { onContinue(state.imagePath) },
                    onBack = onBack,
                )

            ScanUiState.Cancelled ->
                ScanMessage(
                    title = stringResource(R.string.scan_cancelled_title),
                    message = stringResource(R.string.scan_cancelled_message),
                    onRetry = restart,
                    onBack = onBack,
                )

            is ScanUiState.Error ->
                ScanMessage(
                    title = stringResource(R.string.scan_error_title),
                    message = state.error.toDisplayMessage(),
                    onRetry = restart,
                    onBack = onBack,
                )
        }
    }
}

/**
 * Message localisé associé à une [ScanError]. Résolu ici (dans un
 * composable, via [stringResource]) plutôt que dans [ScanError] lui-même,
 * qui reste un type Kotlin pur sans dépendance Android — voir son KDoc.
 * [ScanError.LaunchFailed.technicalMessage] n'est volontairement pas
 * affiché : il n'est utile qu'au diagnostic (déjà loggé côté
 * `DocumentScanner.kt`).
 */
@Composable
private fun ScanError.toDisplayMessage(): String =
    when (this) {
        ScanError.NoImageReturned -> stringResource(R.string.scan_error_no_image)
        ScanError.NoDataReturned -> stringResource(R.string.scan_error_no_data)
        ScanError.InvalidContext -> stringResource(R.string.scan_error_invalid_context)
        is ScanError.LaunchFailed -> stringResource(R.string.scan_error_launch_failed)
    }

@Composable
private fun ScanLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.scan_loading_message))
    }
}

@Composable
private fun ScanSuccess(
    imagePath: String,
    onRescan: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageState = rememberBitmapFromFile(imagePath)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(stringResource(R.string.scan_success_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (imageState) {
                is ImageLoadState.Loaded ->
                    Image(
                        bitmap = imageState.bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.scan_success_image_description),
                        modifier = Modifier.fillMaxSize(),
                    )
                ImageLoadState.Loading -> CircularProgressIndicator()
                ImageLoadState.Failed -> Text(stringResource(R.string.scan_image_load_failed))
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRescan,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(stringResource(R.string.scan_rescan_button))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(stringResource(R.string.scan_continue_button))
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}

@Composable
private fun ScanMessage(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
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
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
        ) {
            Text(stringResource(R.string.scan_retry_button))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}
