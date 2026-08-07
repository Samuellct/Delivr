package com.delivr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.delivr.app.navigation.DelivrNavGraph
import com.delivr.app.ui.theme.DelivrTheme

/**
 * Point d'entrée unique de l'application. Toute la navigation est gérée
 * par [DelivrNavGraph] via Jetpack Compose Navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DelivrTheme {
                // safeDrawingPadding() appliqué une seule fois ici : enableEdgeToEdge()
                // dessine sous les barres système, donc sans ça le contenu des écrans
                // (ex. ScanSuccess) peut passer sous la barre de statut ou de
                // navigation. Poser le padding au niveau racine évite de le répéter
                // dans chaque écran.
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    DelivrNavGraph()
                }
            }
        }
    }
}
