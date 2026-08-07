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
- Google ML Kit Document Scanner (capture, détection des bords, redressement
  de perspective, amélioration du contraste — remplace CameraX, voir
  `TODO_V1.md` § 1 pour le détail de cette décision)
- Google ML Kit OCR (reconnaissance de texte, hors ligne)
- Room (sauvegarde locale de la tournée en cours)

## État actuel — v0.1.0

Structure Gradle, thème Compose, navigation, écran d'accueil fonctionnel, et
écran de scan opérationnel (déclenchement automatique du scanner ML Kit,
aperçu du document redressé, gestion des cas d'annulation/erreur). L'OCR, la
validation et le mode livraison seront ajoutés au fil des prochains commits
(voir `app/src/main/java/com/delivr/app/`, dossiers `ocr`, `data`, `domain`,
`repository`, `database`).

## Ouvrir le projet

1. Ouvrir le dossier dans Android Studio (Ladybug ou plus récent).
2. Laisser Gradle synchroniser (le wrapper est déjà configuré, Gradle 8.13
   / AGP 8.13.2 / Kotlin 2.0.21).
3. Lancer sur un appareil ou émulateur API 26+.

## Structure

```
app/src/main/java/com/delivr/app/
 ├── ui/            écrans Compose (home, scan, ...) + thème
 ├── navigation/     graphe de navigation
 ├── data/           sources de données (à venir)
 ├── domain/         logique métier (tri, extraction, à venir)
 ├── camera/         lancement du scanner ML Kit (DocumentScanner.kt)
 ├── ocr/            intégration ML Kit Text Recognition (à venir)
 ├── repository/     repositories (à venir)
 ├── database/       Room (à venir)
 └── utils/          utilitaires (chargement d'image, BitmapLoader.kt)
```

## Workflow Git

Une seule branche (`main`), commits atomiques, une fonctionnalité = un
commit. Versionning sémantique lisible (`0.1.0`, `0.2.0`, ... `1.0.0`).

## Signature (release)

Le keystore de release (`keystore/delivr-release.jks`) et ses identifiants
(`keystore.properties`) sont gitignorés — ce sont des secrets, jamais
commités. `app/build.gradle.kts` les charge automatiquement s'ils sont
présents ; sans eux, seul le build debug fonctionne. En cas de nouveau poste
de travail, copier ces deux éléments manuellement (jamais par Git).
