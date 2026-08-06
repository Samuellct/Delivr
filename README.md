# Delivr

Application Android native pour automatiser la préparation des tournées de
livraison de petits-déjeuners dans un centre de loisirs. L'app scanne la
feuille de livraison A4 du jour, en extrait les numéros de cottages (colonne
« Cott ») par OCR, les trie dans l'ordre de la tournée, puis guide l'usager
cottage par cottage.

100 % hors ligne : pas de serveur, pas de cloud, pas de compte.

## Stack technique

- Kotlin + Jetpack Compose
- Architecture MVVM
- CameraX (capture + correction de perspective)
- Google ML Kit OCR (reconnaissance de texte, hors ligne)
- Room (sauvegarde locale de la tournée en cours)

## État actuel — v0.1.0

Squelette du projet : structure Gradle, thème Compose, navigation, et écran
d'accueil fonctionnel (boutons "Nouvelle tournée" / "Reprendre la tournée en
cours"). Le scan, l'OCR, la validation et le mode livraison seront ajoutés
au fil des prochains commits (voir `app/src/main/java/com/delivr/app/`,
dossiers `camera`, `ocr`, `data`, `domain`, `repository`, `database`).

## Ouvrir le projet

1. Ouvrir le dossier dans Android Studio (Ladybug ou plus récent).
2. Laisser Gradle synchroniser (le wrapper est déjà configuré, Gradle 8.11.1
   / AGP 8.7.2 / Kotlin 2.0.21).
3. Lancer sur un appareil ou émulateur API 26+.

## Structure

```
app/src/main/java/com/delivr/app/
 ├── ui/            écrans Compose (home, scan, ...) + thème
 ├── navigation/     graphe de navigation
 ├── data/           sources de données (à venir)
 ├── domain/         logique métier (tri, extraction, à venir)
 ├── camera/         capture CameraX (à venir)
 ├── ocr/            intégration ML Kit (à venir)
 ├── repository/     repositories (à venir)
 ├── database/       Room (à venir)
 └── utils/          utilitaires (à venir)
```

## Workflow Git

Une seule branche (`main`), commits atomiques, une fonctionnalité = un
commit. Versionning sémantique lisible (`0.1.0`, `0.2.0`, ... `1.0.0`).
