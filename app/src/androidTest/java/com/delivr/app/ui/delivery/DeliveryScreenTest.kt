package com.delivr.app.ui.delivery

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.delivr.app.R
import com.delivr.app.domain.Cottage
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.domain.goBackToPreviousCottage
import com.delivr.app.domain.markCurrentCottage
import com.delivr.app.ui.theme.DelivrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de rendu et d'interaction de [DeliveryScreen] seul (sur le modèle
 * de `ValidationScreenTest`) : l'écran est une fonction pure de `uiState` +
 * callbacks, donc piloté ici directement avec un état local et les
 * fonctions pures de `domain/DeliveryProgress.kt` — sans ViewModel ni Room.
 *
 * **Ne tourne jamais en CI** (pas d'émulateur, voir `ci.yml`) : la
 * protection automatisée du garde-fou tap/appui long et de la logique de
 * position vient de `DeliveryProgressTest`/`DeliveryViewModelTest` (JVM). Ce
 * fichier sert la confiance au niveau UI, en local, sur appareil réel.
 */
@RunWith(AndroidJUnit4::class)
class DeliveryScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(initialNumbers: List<Int>) {
        composeTestRule.setContent {
            var uiState by
                remember {
                    mutableStateOf<DeliveryUiState>(
                        DeliveryUiState.InProgress(initialNumbers.map { Cottage(it) }),
                    )
                }

            DelivrTheme {
                DeliveryScreen(
                    uiState = uiState,
                    onDelivered = {
                        val current = uiState as DeliveryUiState.InProgress
                        uiState = DeliveryUiState.InProgress(markCurrentCottage(current.cottages, CottageStatus.LIVRE))
                    },
                    onCancelled = {
                        val current = uiState as DeliveryUiState.InProgress
                        uiState = DeliveryUiState.InProgress(markCurrentCottage(current.cottages, CottageStatus.ANNULE))
                    },
                    onPreviousCottage = {
                        val current = uiState as DeliveryUiState.InProgress
                        uiState = DeliveryUiState.InProgress(goBackToPreviousCottage(current.cottages))
                    },
                    onListRequested = {},
                    onBackToHome = {},
                )
            }
        }
    }

    @Test
    fun tap_livre_avance_a_la_position_suivante() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule.onNodeWithText(formatCottageNumber(3)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 1, 3)).assertExists()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_delivered_content_description))
            .performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 2, 3)).assertExists()
    }

    @Test
    fun tap_simple_sur_annule_ne_marque_rien() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_cancelled_content_description))
            .performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(3)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 1, 3)).assertExists()
    }

    @Test
    fun appui_long_sur_annule_avance_a_la_position_suivante() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_cancelled_content_description))
            .performTouchInput { longClick() }

        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 2, 3)).assertExists()
    }

    @Test
    fun retour_recule_d_une_position() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_delivered_content_description))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_delivered_content_description))
            .performClick()
        composeTestRule.onNodeWithText(formatCottageNumber(143)).assertExists()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_previous_content_description))
            .performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 2, 3)).assertExists()
    }

    @Test
    fun retour_sur_le_premier_cottage_ne_fait_rien() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_previous_content_description))
            .performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(3)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 1, 3)).assertExists()
    }

    @Test
    fun l_icone_liste_est_presente_avec_sa_description() {
        setScreen(listOf(3, 35))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_list_content_description))
            .assertExists()
    }

    @Test
    fun fin_de_tournee_affiche_le_message_et_neutralise_livre_et_annule() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        repeat(3) {
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delivery_delivered_content_description))
                .performClick()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.delivery_finished_message)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_position, 3, 3)).assertExists()

        // Ni le tap ni l'appui long ne changent plus rien une fois terminé.
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_delivered_content_description))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_cancelled_content_description))
            .performTouchInput { longClick() }
        composeTestRule.onNodeWithText(context.getString(R.string.delivery_finished_message)).assertExists()

        // Retour reste actif et rattrape le dernier cottage.
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delivery_previous_content_description))
            .performClick()
        composeTestRule.onNodeWithText(formatCottageNumber(143)).assertExists()
    }
}
