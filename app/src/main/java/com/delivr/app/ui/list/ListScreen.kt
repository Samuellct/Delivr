package com.delivr.app.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.domain.Cottage
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.ui.DelivrViewModelFactories

/**
 * Point d'entrée de la destination « liste » (Phase 7), sur le modèle de
 * `HomeRoute`/`DeliveryRoute` : le ViewModel vit ici, [ListScreen] reste une
 * fonction pure de son état.
 */
@Composable
fun ListRoute(
    onBack: () -> Unit,
    onCottageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListViewModel = viewModel(factory = DelivrViewModelFactories.list),
) {
    // Pas de garde "si Loading" (contrairement à Delivery/Validation) : on
    // revient souvent ici après avoir corrigé un statut ailleurs (navigation
    // rapide), la liste doit refléter l'état réel à chaque arrivée, pas un
    // instantané périmé — voir le KDoc de `ListViewModel.load`.
    LaunchedEffect(Unit) { viewModel.load() }

    ListScreen(
        uiState = viewModel.uiState,
        onBack = onBack,
        onCottageSelected = onCottageSelected,
        modifier = modifier,
    )
}

/**
 * Écran Liste (`Presentation.md` § Liste) : vue d'ensemble de tous les
 * cottages de la tournée avec leur statut, et navigation rapide vers l'un
 * d'eux en mode Livraison (`TODO_V1.md` 7.2), quel que soit son statut.
 *
 * Fonction pure de [uiState] et de callbacks, comme `DeliveryScreen`/
 * `ValidationScreen` : `ListScreenTest` la pilote directement, sans
 * ViewModel ni Room.
 */
@Composable
fun ListScreen(
    uiState: ListUiState,
    onBack: () -> Unit,
    onCottageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Pas de Scaffold/TopAppBar : même discipline que ValidationScreen
        // (MainActivity applique déjà les insets système globalement) — une
        // simple Row en tient lieu.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.list_back_content_description),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.list_title), style = MaterialTheme.typography.titleLarge)
        }

        when (uiState) {
            ListUiState.Loading -> ListLoading()
            is ListUiState.Success -> ListContent(cottages = uiState.cottages, onCottageSelected = onCottageSelected)
            is ListUiState.Error -> ListErrorMessage(message = uiState.error.toDisplayMessage())
        }
    }
}

@Composable
private fun ListError.toDisplayMessage(): String =
    when (this) {
        ListError.RoundUnavailable -> stringResource(R.string.list_error_round_unavailable)
    }

@Composable
private fun ListLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.list_loading_message))
    }
}

@Composable
private fun ListContent(
    cottages: List<Cottage>,
    onCottageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(cottages, key = { it.number }) { cottage ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCottageSelected(cottage.number) }
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatCottageNumber(cottage.number), style = MaterialTheme.typography.headlineSmall)
                Text(
                    cottage.status.toDisplayLabel(),
                    color = cottage.status.toDisplayColor(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CottageStatus.toDisplayLabel(): String =
    when (this) {
        CottageStatus.A_FAIRE -> stringResource(R.string.list_status_a_faire)
        CottageStatus.LIVRE -> stringResource(R.string.list_status_livre)
        CottageStatus.ANNULE -> stringResource(R.string.list_status_annule)
    }

/** Mêmes couleurs que le mode Livraison (`DeliveryScreen.kt`) : vert pour Livré, rouge pour Annulé. */
@Composable
private fun CottageStatus.toDisplayColor(): Color =
    when (this) {
        CottageStatus.A_FAIRE -> MaterialTheme.colorScheme.onSurfaceVariant
        CottageStatus.LIVRE -> MaterialTheme.colorScheme.tertiary
        CottageStatus.ANNULE -> MaterialTheme.colorScheme.error
    }

/**
 * Cas défensif : on n'atteint normalement cet écran que depuis une tournée
 * existante. Pas de bouton de retour ici (contrairement à
 * `DeliveryErrorMessage`/`ValidationErrorMessage`) : la flèche déjà présente
 * dans la barre du haut (visible dans tous les états de cet écran) suffit —
 * en ajouter un second créerait deux affordances de retour redondantes.
 */
@Composable
private fun ListErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.list_error_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
