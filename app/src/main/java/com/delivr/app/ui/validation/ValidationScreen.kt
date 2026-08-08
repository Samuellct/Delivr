package com.delivr.app.ui.validation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.domain.formatCottageNumber

/**
 * Point d'entrée de la destination de navigation "validation" : c'est ici
 * qu'est déclenchée l'extraction, sur le modèle de `ScanRoute`/`ScanScreen`
 * (séparation entre le point d'entrée qui orchestre l'effet de bord et
 * l'écran, pur et testable).
 */
@Composable
fun ValidationRoute(
    imagePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ValidationViewModel = viewModel(),
) {
    LaunchedEffect(imagePath) {
        // Conditionné à Extracting : après une rotation, un résultat déjà
        // obtenu (Success/Error) est restauré depuis SavedStateHandle et ne
        // doit pas être refait inutilement (voir ValidationViewModel.kt).
        if (viewModel.uiState is ValidationUiState.Extracting) {
            viewModel.extract(imagePath)
        }
    }

    ValidationScreen(
        uiState = viewModel.uiState,
        onRetry = { viewModel.extract(imagePath) },
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Écran de validation — **volontairement minimal pour la Phase 3** : affiche
 * la liste triée des numéros de cottage détectés, en lecture seule. Modifier/
 * supprimer/ajouter un cottage et choisir le sens de la tournée sont
 * l'objet de la Phase 4, qui viendra enrichir cet écran plutôt que d'en
 * créer un nouveau.
 */
@Composable
fun ValidationScreen(
    uiState: ValidationUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            ValidationUiState.Extracting -> ValidationLoading()

            is ValidationUiState.Success ->
                ValidationResult(
                    cottageNumbers = uiState.cottageNumbers,
                    onBack = onBack,
                )

            is ValidationUiState.Error ->
                ValidationErrorMessage(
                    message = uiState.error.toDisplayMessage(),
                    onRetry = onRetry,
                    onBack = onBack,
                )
        }
    }
}

@Composable
private fun ValidationError.toDisplayMessage(): String =
    when (this) {
        ValidationError.ImageUnreadable -> stringResource(R.string.validation_error_image_unreadable)
        ValidationError.HeaderNotFound -> stringResource(R.string.validation_error_header_not_found)
        ValidationError.NoNumbersFound -> stringResource(R.string.validation_error_no_numbers_found)
    }

@Composable
private fun ValidationLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.validation_loading_message))
    }
}

@Composable
private fun ValidationResult(
    cottageNumbers: List<Int>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.validation_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.validation_count, cottageNumbers.size),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cottageNumbers) { number ->
                Text(
                    formatCottageNumber(number),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}

@Composable
private fun ValidationErrorMessage(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.validation_error_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.validation_retry_button))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}
