package com.delivr.app.ui.validation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delivr.app.R
import com.delivr.app.domain.SortDirection
import com.delivr.app.domain.formatCottageNumber
import com.delivr.app.ui.DelivrViewModelFactories

private const val MIN_COTTAGE_NUMBER = 1
private const val MAX_COTTAGE_NUMBER = 999
private const val MAX_DIGITS = 3

/** Repère le champ de saisie de [CottageNumberDialog] pour `ValidationScreenTest`. */
const val COTTAGE_NUMBER_FIELD_TAG = "cottage_number_field"

/**
 * Point d'entrée de la destination de navigation "validation" : c'est ici
 * qu'est déclenchée l'extraction, sur le modèle de `ScanRoute`/`ScanScreen`
 * (séparation entre le point d'entrée qui orchestre l'effet de bord et
 * l'écran, pur et testable).
 *
 * [imagePath] est redevenu non nullable en Phase 6 : il n'existe plus qu'un
 * seul chemin vers cet écran (un scan qui vient d'aboutir). La reprise d'une
 * tournée sauvegardée mène désormais directement au mode Livraison
 * (`Routes.DELIVERY`), pas ici.
 */
@Composable
fun ValidationRoute(
    imagePath: String,
    onBack: () -> Unit,
    onStartDelivery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ValidationViewModel = viewModel(factory = DelivrViewModelFactories.validation),
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
        onStartDelivery = onStartDelivery,
        onAdd = viewModel::onAddCottage,
        onRemove = viewModel::onRemoveCottage,
        onUpdate = viewModel::onUpdateCottage,
        onSortDirectionChange = viewModel::onSortDirectionChange,
        modifier = modifier,
    )
}

/**
 * Écran de validation : liste des numéros de cottage détectés, modifiable
 * (Phase 4) — ajouter, supprimer, modifier un numéro, choisir le sens de
 * tournée (`Presentation.md` §§ 4-5).
 *
 * Reste une fonction pure de [uiState] et de callbacks (pas du ViewModel
 * directement, à la différence de `ScanScreen`) : ça permet de piloter les
 * tests Compose UI (`ValidationScreenTest.kt`) sans ViewModel ni
 * `SavedStateHandle`.
 */
@Composable
fun ValidationScreen(
    uiState: ValidationUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onStartDelivery: () -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (oldNumber: Int, newNumber: Int) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            ValidationUiState.Extracting -> ValidationLoading()

            is ValidationUiState.Success ->
                ValidationResult(
                    cottageNumbers = uiState.cottageNumbers,
                    sortDirection = uiState.sortDirection,
                    onBack = onBack,
                    onStartDelivery = onStartDelivery,
                    onAdd = onAdd,
                    onRemove = onRemove,
                    onUpdate = onUpdate,
                    onSortDirectionChange = onSortDirectionChange,
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
        ValidationError.RoundUnavailable -> stringResource(R.string.validation_error_round_unavailable)
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

/** Cible du dialogue partagé d'ajout/modification (voir [CottageNumberDialog]). */
private sealed interface DialogTarget {
    data object Add : DialogTarget

    data class Edit(
        val number: Int,
    ) : DialogTarget
}

@Composable
private fun ValidationResult(
    cottageNumbers: List<Int>,
    sortDirection: SortDirection,
    onBack: () -> Unit,
    onStartDelivery: () -> Unit,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (oldNumber: Int, newNumber: Int) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogTarget by remember { mutableStateOf<DialogTarget?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Marge basse généreuse : le bouton flottant d'ajout ne
                    // doit pas recouvrir les deux boutons du bas
                    // (« Démarrer la tournée » et « Retour à l'accueil »).
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        ) {
            Text(stringResource(R.string.validation_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.validation_count, cottageNumbers.size),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { onSortDirectionChange(SortDirection.ASCENDING) },
                    selected = sortDirection == SortDirection.ASCENDING,
                ) {
                    Text(stringResource(R.string.validation_sort_ascending))
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { onSortDirectionChange(SortDirection.DESCENDING) },
                    selected = sortDirection == SortDirection.DESCENDING,
                ) {
                    Text(stringResource(R.string.validation_sort_descending))
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cottageNumbers, key = { it }) { number ->
                    CottageRow(
                        number = number,
                        onClick = { dialogTarget = DialogTarget.Edit(number) },
                        onDelete = { onRemove(number) },
                    )
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStartDelivery,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.validation_start_delivery_button))
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_to_home))
            }
        }

        FloatingActionButton(
            onClick = { dialogTarget = DialogTarget.Add },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.validation_add_fab_content_description))
        }
    }

    dialogTarget?.let { target ->
        CottageNumberDialog(
            initialValue = (target as? DialogTarget.Edit)?.number,
            existingNumbers = cottageNumbers.toSet(),
            onConfirm = { newNumber ->
                when (target) {
                    is DialogTarget.Edit -> onUpdate(target.number, newNumber)
                    DialogTarget.Add -> onAdd(newNumber)
                }
                dialogTarget = null
            },
            onDismiss = { dialogTarget = null },
        )
    }
}

@Composable
private fun CottageRow(
    number: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatCottageNumber(number),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription =
                    stringResource(R.string.validation_delete_content_description, formatCottageNumber(number)),
            )
        }
    }
}

/**
 * Dialogue partagé d'ajout ([initialValue] `null`) et de modification
 * ([initialValue] non nul) d'un numéro de cottage. Validation en direct :
 * bouton de confirmation désactivé si la valeur est hors de `1..999` ou si
 * elle existe déjà dans [existingNumbers] — sauf si elle est égale à
 * [initialValue] (modifier un numéro sans le changer n'est pas un doublon).
 */
@Composable
private fun CottageNumberDialog(
    initialValue: Int?,
    existingNumbers: Set<Int>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue?.let(::formatCottageNumber) ?: "") }
    val parsed = text.toIntOrNull()
    val isOutOfRange = parsed == null || parsed !in MIN_COTTAGE_NUMBER..MAX_COTTAGE_NUMBER
    val isDuplicate = parsed != null && parsed != initialValue && parsed in existingNumbers
    val isValid = !isOutOfRange && !isDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initialValue == null) R.string.validation_add_title else R.string.validation_edit_title,
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> if (new.length <= MAX_DIGITS && new.all(Char::isDigit)) text = new },
                    label = { Text(stringResource(R.string.validation_dialog_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = text.isNotEmpty() && !isValid,
                    modifier = Modifier.testTag(COTTAGE_NUMBER_FIELD_TAG),
                )
                if (text.isNotEmpty() && isOutOfRange) {
                    Text(
                        stringResource(R.string.validation_error_out_of_range),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (text.isNotEmpty() && isDuplicate) {
                    Text(
                        stringResource(R.string.validation_error_duplicate),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = isValid) {
                Text(
                    stringResource(
                        if (initialValue == null) R.string.validation_add_button else R.string.validation_save_button,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
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
