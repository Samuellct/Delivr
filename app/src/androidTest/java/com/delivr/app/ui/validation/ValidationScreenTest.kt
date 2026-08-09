package com.delivr.app.ui.validation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.delivr.app.R
import com.delivr.app.domain.SortDirection
import com.delivr.app.domain.addCottageNumber
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.domain.removeCottageNumber
import com.delivr.app.domain.sortCottageNumbers
import com.delivr.app.domain.updateCottageNumber
import com.delivr.app.ui.theme.DelivrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de rendu et d'interaction de [ValidationScreen] seul (sur le modèle
 * de `ScanScreenTest`) : l'écran est une fonction pure de `uiState` +
 * callbacks (voir `ValidationScreen.kt`), donc piloté ici directement avec un
 * état local et les fonctions pures de `domain/CottageList.kt` — sans
 * ViewModel ni `SavedStateHandle`.
 */
@RunWith(AndroidJUnit4::class)
class ValidationScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        initialNumbers: List<Int>,
        initialDirection: SortDirection = SortDirection.ASCENDING,
    ) {
        composeTestRule.setContent {
            var uiState by
                remember {
                    mutableStateOf<ValidationUiState>(
                        ValidationUiState.Success(initialNumbers, initialDirection),
                    )
                }

            DelivrTheme {
                ValidationScreen(
                    uiState = uiState,
                    onRetry = {},
                    onBack = {},
                    onStartDelivery = {},
                    onAdd = { number ->
                        val current = uiState as ValidationUiState.Success
                        uiState = current.copy(cottageNumbers = addCottageNumber(current.cottageNumbers, number, current.sortDirection))
                    },
                    onRemove = { number ->
                        val current = uiState as ValidationUiState.Success
                        uiState = current.copy(cottageNumbers = removeCottageNumber(current.cottageNumbers, number))
                    },
                    onUpdate = { oldNumber, newNumber ->
                        val current = uiState as ValidationUiState.Success
                        uiState =
                            current.copy(
                                cottageNumbers =
                                    updateCottageNumber(current.cottageNumbers, oldNumber, newNumber, current.sortDirection),
                            )
                    },
                    onSortDirectionChange = { direction ->
                        val current = uiState as ValidationUiState.Success
                        uiState =
                            current.copy(
                                cottageNumbers = sortCottageNumbers(current.cottageNumbers, direction),
                                sortDirection = direction,
                            )
                    },
                )
            }
        }
    }

    @Test
    fun ajout_d_un_numero_valide_apparait_trie_au_bon_endroit() {
        setScreen(listOf(3, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.validation_add_fab_content_description))
            .performClick()
        composeTestRule.onNodeWithTag(COTTAGE_NUMBER_FIELD_TAG).performTextInput("35")
        composeTestRule.onNodeWithText(context.getString(R.string.validation_add_button)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.validation_count, 3)).assertExists()
        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
    }

    @Test
    fun suppression_fait_disparaitre_la_ligne_et_met_a_jour_le_compteur() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.validation_delete_content_description, formatCottageNumber(35)),
            ).performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.validation_count, 2)).assertExists()
    }

    @Test
    fun modification_fait_disparaitre_l_ancienne_valeur_et_apparaitre_la_nouvelle_triee() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule.onNodeWithText(formatCottageNumber(35)).performClick()
        composeTestRule.onNodeWithTag(COTTAGE_NUMBER_FIELD_TAG).performTextClearance()
        composeTestRule.onNodeWithTag(COTTAGE_NUMBER_FIELD_TAG).performTextInput("999")
        composeTestRule.onNodeWithText(context.getString(R.string.validation_save_button)).performClick()

        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertDoesNotExist()
        composeTestRule.onNodeWithText(formatCottageNumber(999)).assertExists()
    }

    @Test
    fun changement_de_sens_inverse_l_ordre_affiche() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule.onNodeWithText(context.getString(R.string.validation_sort_descending)).performClick()

        // Les trois nombres restent affichés après le retri : on vérifie
        // simplement que le tri décroissant n'a rien fait disparaître.
        composeTestRule.onNodeWithText(formatCottageNumber(3)).assertExists()
        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
        composeTestRule.onNodeWithText(formatCottageNumber(143)).assertExists()
    }

    @Test
    fun saisie_hors_plage_desactive_le_bouton_et_affiche_un_message() {
        setScreen(listOf(3, 35))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.validation_add_fab_content_description))
            .performClick()
        composeTestRule.onNodeWithTag(COTTAGE_NUMBER_FIELD_TAG).performTextInput("0")

        composeTestRule.onNodeWithText(context.getString(R.string.validation_error_out_of_range)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.validation_add_button)).assertIsNotEnabled()
    }

    @Test
    fun saisie_en_doublon_desactive_le_bouton_et_affiche_un_message() {
        setScreen(listOf(3, 35))
        val context = composeTestRule.activity

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.validation_add_fab_content_description))
            .performClick()
        composeTestRule.onNodeWithTag(COTTAGE_NUMBER_FIELD_TAG).performTextInput("35")

        composeTestRule.onNodeWithText(context.getString(R.string.validation_error_duplicate)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.validation_add_button)).assertIsNotEnabled()
    }

    @Test
    fun modifier_un_numero_sans_le_changer_n_est_pas_bloque_comme_un_doublon() {
        setScreen(listOf(3, 35, 143))
        val context = composeTestRule.activity

        composeTestRule.onNodeWithText(formatCottageNumber(35)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.validation_error_duplicate)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.validation_save_button)).assertIsEnabled()
    }
}
