package com.delivr.app.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.delivr.app.R
import com.delivr.app.domain.Cottage
import com.delivr.app.domain.CottageStatus
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.ui.theme.DelivrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de rendu et d'interaction de [ListScreen] seul (sur le modèle de
 * `DeliveryScreenTest`) : fonction pure de `uiState` + callbacks, pilotée
 * directement avec un état local — sans ViewModel ni Room.
 *
 * **Ne tourne jamais en CI** (pas d'émulateur, voir `ci.yml`) : la
 * protection automatisée vient de `ListViewModelTest` (JVM). Ce fichier sert
 * la confiance au niveau UI, en local, sur appareil réel.
 */
@RunWith(AndroidJUnit4::class)
class ListScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        cottages: List<Cottage>,
        onBack: () -> Unit = {},
        onCottageSelected: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            DelivrTheme {
                ListScreen(
                    uiState = ListUiState.Success(cottages),
                    onBack = onBack,
                    onCottageSelected = onCottageSelected,
                )
            }
        }
    }

    @Test
    fun tous_les_cottages_s_affichent_avec_leur_numero_et_leur_statut() {
        val context = composeTestRule.activity
        setScreen(
            listOf(
                Cottage(3, CottageStatus.LIVRE),
                Cottage(35, CottageStatus.ANNULE),
                Cottage(143, CottageStatus.A_FAIRE),
            ),
        )

        composeTestRule.onNodeWithText(formatCottageNumber(3)).assertExists()
        composeTestRule.onNodeWithText(formatCottageNumber(35)).assertExists()
        composeTestRule.onNodeWithText(formatCottageNumber(143)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.list_status_livre)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.list_status_annule)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.list_status_a_faire)).assertExists()
    }

    @Test
    fun taper_un_cottage_appelle_onCottageSelected_avec_son_numero() {
        var selected: Int? = null
        setScreen(
            listOf(Cottage(3, CottageStatus.A_FAIRE), Cottage(35, CottageStatus.LIVRE)),
            onCottageSelected = { selected = it },
        )

        composeTestRule.onNodeWithText(formatCottageNumber(35)).performClick()

        assert(selected == 35) { "attendu 35, obtenu $selected" }
    }

    @Test
    fun taper_un_cottage_deja_livre_appelle_quand_meme_onCottageSelected() {
        // Le critère d'acceptation de la Phase 7 est explicite : la navigation
        // rapide fonctionne "quel que soit son statut actuel", pas seulement
        // pour les cottages encore à faire.
        var selected: Int? = null
        setScreen(listOf(Cottage(3, CottageStatus.LIVRE)), onCottageSelected = { selected = it })

        composeTestRule.onNodeWithText(formatCottageNumber(3)).performClick()

        assert(selected == 3) { "attendu 3, obtenu $selected" }
    }

    @Test
    fun le_bouton_retour_appelle_onBack() {
        var backCalled = false
        val context = composeTestRule.activity
        setScreen(listOf(Cottage(3, CottageStatus.A_FAIRE)), onBack = { backCalled = true })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.list_back_content_description))
            .performClick()

        assert(backCalled) { "onBack n'a pas été appelé" }
    }
}
