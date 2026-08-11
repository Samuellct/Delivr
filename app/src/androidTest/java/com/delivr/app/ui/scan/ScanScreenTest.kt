package com.delivr.app.ui.scan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.delivr.app.R
import com.delivr.app.camera.ScanError
import com.delivr.app.camera.ScanOutcome
import com.delivr.app.ui.theme.DelivrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de rendu de [ScanScreen] seul, sans ML Kit ni Play services : rendu
 * possible grâce à l'abstraction posée en 1.5 (`startScan` injecté plutôt
 * qu'appelé en interne). L'état est piloté directement via [ScanViewModel],
 * sans passer par un vrai scan.
 */
@RunWith(AndroidJUnit4::class)
class ScanScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScanScreen(
        viewModel: ScanViewModel,
        startScan: () -> Unit = {},
        onContinue: (imagePath: String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            DelivrTheme {
                ScanScreen(
                    onBack = {},
                    onContinue = onContinue,
                    startScan = startScan,
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun etat_idle_affiche_le_message_de_chargement() {
        val viewModel = ScanViewModel(SavedStateHandle())
        setScanScreen(viewModel)

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.scan_loading_message),
            ).assertExists()
    }

    @Test
    fun etat_success_affiche_le_titre_et_les_boutons() {
        val viewModel = ScanViewModel(SavedStateHandle())
        setScanScreen(viewModel)
        viewModel.onScanOutcome(ScanOutcome.Success("/tmp/inexistant.jpg"))

        val context = composeTestRule.activity
        composeTestRule.onNodeWithText(context.getString(R.string.scan_success_title)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.scan_rescan_button)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.scan_continue_button)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.action_back_to_home)).assertExists()
    }

    @Test
    fun tap_sur_continuer_transmet_le_chemin_de_l_image_scannee() {
        val viewModel = ScanViewModel(SavedStateHandle())
        var receivedImagePath: String? = null
        setScanScreen(viewModel, onContinue = { imagePath -> receivedImagePath = imagePath })
        viewModel.onScanOutcome(ScanOutcome.Success("/tmp/inexistant.jpg"))

        val context = composeTestRule.activity
        composeTestRule.onNodeWithText(context.getString(R.string.scan_continue_button)).performClick()

        assertEquals("/tmp/inexistant.jpg", receivedImagePath)
    }

    @Test
    fun etat_cancelled_affiche_le_message_localise_et_relance_le_scan() {
        val viewModel = ScanViewModel(SavedStateHandle())
        // L'état doit être Cancelled AVANT la composition : sinon uiState vaut
        // encore Idle au moment où ScanScreen compose, et son LaunchedEffect(Unit)
        // déclenche un premier startScan() automatique (comportement voulu, voir
        // ScanScreen.kt) avant même que ce test ne force l'annulation — ce qui
        // faussait le compteur ci-dessous.
        viewModel.onScanOutcome(ScanOutcome.Cancelled)
        var startScanCallCount = 0
        setScanScreen(viewModel, startScan = { startScanCallCount++ })

        val context = composeTestRule.activity
        composeTestRule.onNodeWithText(context.getString(R.string.scan_cancelled_title)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.scan_cancelled_message)).assertExists()

        composeTestRule.onNodeWithText(context.getString(R.string.scan_retry_button)).performClick()

        assert(startScanCallCount == 1) {
            "startScan devrait être appelé une fois après un tap sur le bouton de relance, " +
                "appelé $startScanCallCount fois"
        }
    }

    @Test
    fun etat_error_affiche_un_message_localise_jamais_le_texte_ml_kit_brut() {
        val viewModel = ScanViewModel(SavedStateHandle())
        setScanScreen(viewModel)
        viewModel.onScanOutcome(ScanOutcome.Error(ScanError.LaunchFailed("NullPointerException at line 42")))

        val context = composeTestRule.activity
        composeTestRule.onNodeWithText(context.getString(R.string.scan_error_title)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.scan_error_launch_failed)).assertExists()
        composeTestRule.onNodeWithText("NullPointerException at line 42").assertDoesNotExist()
    }
}
