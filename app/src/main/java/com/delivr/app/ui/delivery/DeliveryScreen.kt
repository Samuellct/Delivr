package com.delivr.app.ui.delivery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.ui.DelivrViewModelFactories

/**
 * Taille des deux commandes principales (Livré/Annulé) : nettement plus
 * grande que la cible tactile minimale de 48.dp — ce sont les gestes les
 * plus fréquents du mode Livraison (un par cottage), donc ceux dont le
 * confort compte le plus. Retour/Liste restent à la taille par défaut
 * (`IconButton`) : gestes secondaires, moins fréquents.
 */
private val PRIMARY_ACTION_SIZE = 72.dp
private val PRIMARY_ACTION_ICON_SIZE = 40.dp

/**
 * Point d'entrée de la destination « livraison » (Phase 6), sur le modèle de
 * `HomeRoute`/`ValidationRoute` : le ViewModel vit ici, [DeliveryScreen]
 * reste une fonction pure de son état.
 *
 * La tournée est **toujours** relue depuis Room, qu'on arrive depuis
 * « Démarrer la tournée » (après un scan), depuis « Reprendre la tournée en
 * cours » (accueil), ou depuis l'écran Liste (Phase 7) — [focusOnCottageNumber]
 * n'est renseigné que dans ce dernier cas, pour afficher ce cottage précis
 * plutôt que le courant déduit (voir `DeliveryViewModel.load`).
 */
@Composable
fun DeliveryRoute(
    onBackToHome: () -> Unit,
    onListRequested: () -> Unit,
    modifier: Modifier = Modifier,
    focusOnCottageNumber: Int? = null,
    viewModel: DeliveryViewModel = viewModel(factory = DelivrViewModelFactories.delivery),
) {
    LaunchedEffect(Unit) {
        // Conditionné à Loading, comme ValidationRoute : après un changement
        // de configuration, le ViewModel a survécu avec son état, inutile de
        // relire la base.
        if (viewModel.uiState is DeliveryUiState.Loading) {
            viewModel.load(focusOnCottageNumber)
        }
    }

    DeliveryScreen(
        uiState = viewModel.uiState,
        onDelivered = viewModel::onDelivered,
        onCancelled = viewModel::onCancelled,
        onPreviousCottage = viewModel::onPreviousCottage,
        onListRequested = onListRequested,
        onBackToHome = onBackToHome,
        modifier = modifier,
    )
}

/**
 * Mode Livraison (`Presentation.md` § Mode Livraison) : le numéro du cottage
 * courant en très gros au centre, sa position (« 6 / 24 »), et quatre
 * commandes dans les quatre coins (`TODO_V1.md` 6.2) — Retour en haut à
 * gauche, Liste en haut à droite, Livré en bas à gauche, Annulé en bas à
 * droite. Les coins sont choisis pour rester atteignables au pouce sur un
 * téléphone tenu d'une main.
 *
 * Fonction pure de [uiState] et de callbacks, comme `ValidationScreen` :
 * `DeliveryScreenTest` la pilote directement, sans ViewModel ni Room.
 */
@Composable
fun DeliveryScreen(
    uiState: DeliveryUiState,
    onDelivered: () -> Unit,
    onCancelled: () -> Unit,
    onPreviousCottage: () -> Unit,
    onListRequested: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            DeliveryUiState.Loading -> DeliveryLoading()

            is DeliveryUiState.InProgress ->
                DeliveryInProgress(
                    state = uiState,
                    onDelivered = onDelivered,
                    onCancelled = onCancelled,
                    onPreviousCottage = onPreviousCottage,
                    onListRequested = onListRequested,
                )

            is DeliveryUiState.Error ->
                DeliveryErrorMessage(
                    message = uiState.error.toDisplayMessage(),
                    onBackToHome = onBackToHome,
                )
        }
    }
}

@Composable
private fun DeliveryError.toDisplayMessage(): String =
    when (this) {
        DeliveryError.RoundUnavailable -> stringResource(R.string.delivery_error_round_unavailable)
    }

@Composable
private fun DeliveryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.delivery_loading_message))
    }
}

@Composable
private fun DeliveryInProgress(
    state: DeliveryUiState.InProgress,
    onDelivered: () -> Unit,
    onCancelled: () -> Unit,
    onPreviousCottage: () -> Unit,
    onListRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // ---- Centre : libellé, numéro, position -------------------------
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.delivery_cottage_label),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            val current = state.currentCottage
            if (current == null) {
                Text(
                    stringResource(R.string.delivery_finished_message),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    formatCottageNumber(current.number),
                    // Nettement plus grand que displayLarge (57.sp) : le
                    // numéro doit être lisible d'un coup d'œil, à bout de
                    // bras (Presentation.md § Mode Livraison, retour
                    // utilisateur Phase 6).
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp, fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.delivery_position, state.displayPosition, state.total),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // ---- Coin haut-gauche : Retour ----------------------------------
        IconButton(
            onClick = onPreviousCottage,
            enabled = state.canGoBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(
                // AutoMirrored : la flèche se retourne d'elle-même en
                // disposition droite-à-gauche. `Icons.Filled.ArrowBack` est
                // déprécié pour cette raison.
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.delivery_previous_content_description),
            )
        }

        // ---- Coin haut-droit : Liste (Phase 7) --------------------------
        IconButton(
            onClick = onListRequested,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.delivery_list_content_description),
            )
        }

        // ---- Coin bas-gauche : Livré (tap simple) -----------------------
        IconButton(
            onClick = onDelivered,
            enabled = !state.isFinished,
            // contentColor suffit : IconButtonDefaults en dérive tout seul
            // la couleur désactivée (même teinte à 38 % d'opacité).
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(PRIMARY_ACTION_SIZE),
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.delivery_delivered_content_description),
                modifier = Modifier.size(PRIMARY_ACTION_ICON_SIZE),
            )
        }

        // ---- Coin bas-droit : Annulé (appui long uniquement) ------------
        CancelControl(
            enabled = !state.isFinished,
            onCancelled = onCancelled,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/**
 * Garde-fou de `TODO_V1.md` 6.3 : un tap simple sur la croix rouge ne doit
 * **rien** faire, seul un appui long annule le cottage — un tap involontaire
 * sur un téléphone tenu d'une main ne doit pas faire avancer la tournée par
 * erreur. Pas de boîte de confirmation, qui ralentirait aussi le cas normal.
 *
 * D'où un [Box] + [combinedClickable] plutôt qu'un `IconButton` : ce dernier
 * n'expose pas d'appui long. Le [Box] fait 48.dp (cible tactile minimale
 * recommandée par Material) et centre l'icône, ce que l'`IconButton` faisait
 * gratuitement. Le retour haptique remplace le retour visuel d'un dialogue :
 * l'utilisateur sent que l'appui long a « pris ».
 *
 * Double étiquetage volontaire pour TalkBack : la description de l'icône dit
 * ce que c'est, `onLongClickLabel` dit ce que fait l'appui long.
 *
 * Taille alignée sur [PRIMARY_ACTION_SIZE] (au lieu de la cible tactile
 * minimale de 48.dp) : même confort d'usage que le bouton Livré, sur
 * lequel un retour utilisateur a demandé plus de place pour les deux
 * commandes principales.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CancelControl(
    enabled: Boolean,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val longClickLabel = stringResource(R.string.delivery_cancelled_long_click_label)
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .size(PRIMARY_ACTION_SIZE)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = longClickLabel,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancelled()
                    },
                    onClick = { /* absorbe volontairement les taps accidentels */ },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = stringResource(R.string.delivery_cancelled_content_description),
            tint = if (enabled) errorColor else errorColor.copy(alpha = 0.38f),
            modifier = Modifier.size(PRIMARY_ACTION_ICON_SIZE),
        )
    }
}

/**
 * Cas défensif : on n'atteint normalement cet écran que depuis une tournée
 * existante. Une porte de sortie explicite est quand même offerte (même
 * discipline que `ValidationErrorMessage`), pour ne jamais laisser un écran
 * sans issue autre que le retour système.
 */
@Composable
private fun DeliveryErrorMessage(
    message: String,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.delivery_error_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBackToHome) {
            Text(stringResource(R.string.action_back_to_home))
        }
    }
}
