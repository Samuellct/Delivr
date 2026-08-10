package com.delivr.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.delivr.app.DelivrApplication
import com.delivr.app.ui.delivery.DeliveryViewModel
import com.delivr.app.ui.home.HomeViewModel
import com.delivr.app.ui.list.ListViewModel
import com.delivr.app.ui.validation.ValidationViewModel

/**
 * Fabriques des ViewModels qui ont besoin du repository Room. Elles
 * remplacent la fabrique par défaut (`SavedStateViewModelFactory`), qui sait
 * construire un ViewModel ne prenant qu'un `SavedStateHandle` — ce qui
 * suffisait jusqu'ici (voir `ScanViewModel`) mais plus dès qu'un second
 * paramètre apparaît.
 *
 * `ScanViewModel` n'est pas concerné : le scan ne touche pas la base. Son
 * `viewModel()` nu, dans `ScanRoute`/`ScanScreen`, reste inchangé.
 *
 * Le DSL `viewModelFactory { initializer { … } }` donne accès aux
 * `CreationExtras` : `APPLICATION_KEY` fournit l'Application (donc le
 * repository) et `createSavedStateHandle()` reconstruit le `SavedStateHandle`
 * attaché à l'entrée de navigation courante — ce qui préserve intégralement
 * le mécanisme de survie à la rotation / mort de process de la Phase 1.
 */
object DelivrViewModelFactories {
    val home: ViewModelProvider.Factory =
        viewModelFactory {
            initializer { HomeViewModel(repository = delivrApplication().roundRepository) }
        }

    val validation: ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                ValidationViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = delivrApplication().roundRepository,
                )
            }
        }

    /**
     * Pas de `createSavedStateHandle()` ici : `DeliveryViewModel` n'en a pas
     * besoin (voir son KDoc — Room est déjà la source de vérité de la
     * progression, contrairement à l'extraction OCR de `ValidationViewModel`).
     */
    val delivery: ViewModelProvider.Factory =
        viewModelFactory {
            initializer { DeliveryViewModel(repository = delivrApplication().roundRepository) }
        }

    /** Même raisonnement que [delivery] : `ListViewModel` n'a besoin que du repository. */
    val list: ViewModelProvider.Factory =
        viewModelFactory {
            initializer { ListViewModel(repository = delivrApplication().roundRepository) }
        }
}

private fun CreationExtras.delivrApplication(): DelivrApplication {
    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    return application as? DelivrApplication
        ?: error("Application absente des CreationExtras ou non DelivrApplication : vérifier android:name du manifeste")
}
