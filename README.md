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
commit.

Les messages de commit suivent la convention
[Conventional Commits](https://www.conventionalcommits.org/) : c'est ce que
lit le workflow de release pour calculer automatiquement le prochain numéro
de version.

- `feat: ...` → nouvelle version **mineure** (`0.1.0` → `0.2.0`)
- `fix: ...` → nouvelle version de **patch** (`0.1.0` → `0.1.1`)
- `feat!: ...` ou un pied de commit `BREAKING CHANGE: ...` → version
  **majeure** (`0.1.0` → `1.0.0`)
- `docs:`, `chore:`, `refactor:`, `test:`, ... pour tout le reste

## Intégration et livraison continues (GitHub Actions)

Deux workflows tournent sur `.github/workflows/` :

- **`ci.yml`** — à chaque push/PR sur `main` : tests unitaires, lint, build
  debug. Produit une APK debug téléchargeable en artifact d'Action (30 jours).
- **`release.yml`** — à chaque push sur `main` : calcule le prochain tag
  SemVer à partir des Conventional Commits, build une **APK release
  signée**, publie une GitHub Release avec l'APK attachée et le changelog
  généré, puis met à jour `CHANGELOG.md`.

Pour tester une nouvelle version sur un téléphone : ouvrir la dernière
[GitHub Release](https://github.com/Samuellct/Delivr/releases), télécharger
l'APK depuis le téléphone, l'installer (elle s'installe par-dessus la
précédente sans perte de données, même `applicationId`, même clé de
signature).

## Signature (release)

Le keystore de release (`keystore/delivr-release.jks`) et ses identifiants
(`keystore.properties`) sont gitignorés — ce sont des secrets, jamais
commités. `app/build.gradle.kts` résout la configuration de signature dans
cet ordre :

1. Variables d'environnement `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` /
   `KEY_ALIAS` / `KEY_PASSWORD` — utilisées par `release.yml`, qui
   reconstitue le fichier `.jks` à partir du secret GitHub `KEYSTORE_BASE64`
   (jamais commité, jamais loggé).
2. `keystore.properties` en local, pour signer une release depuis un poste
   de développement.
3. Sans les deux : le build release reste non signé (le debug fonctionne
   toujours).

En cas de nouveau poste de travail, copier `keystore/` et
`keystore.properties` manuellement (jamais par Git).
