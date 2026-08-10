
## [v0.6.7] - 2026-08-10
### [0.6.7](https://github.com/Samuellct/Delivr/compare/v0.6.6...v0.6.7) (2026-08-10)



## [v0.6.6] - 2026-08-10
### [0.6.6](https://github.com/Samuellct/Delivr/compare/v0.6.5...v0.6.6) (2026-08-10)


### Build Systems

* met à jour le Compose BOM ([efd760a](https://github.com/Samuellct/Delivr/commit/efd760a0a998069d71cfc87d40c396335d4f63a2))



## [v0.6.5] - 2026-08-10
### [0.6.5](https://github.com/Samuellct/Delivr/compare/v0.6.4...v0.6.5) (2026-08-10)


### Build Systems

* met à jour les dépendances AndroidX ([50dbd73](https://github.com/Samuellct/Delivr/commit/50dbd737c077e3d268bd6aadc632d193024f9811))



## [v0.6.3] - 2026-08-10
### [0.6.3](https://github.com/Samuellct/Delivr/compare/v0.6.2...v0.6.3) (2026-08-10)


### Build Systems

* installe les builds debug et release côte à côte ([ce1c1ff](https://github.com/Samuellct/Delivr/commit/ce1c1fff25b96a63149959d3c8d256d485843b2e))



## [v0.6.2] - 2026-08-10
### [0.6.2](https://github.com/Samuellct/Delivr/compare/v0.6.1...v0.6.2) (2026-08-10)


### Bug Fixes

* **delivery:** navigation rapide par position (défilement libre) ([c2d5dc7](https://github.com/Samuellct/Delivr/commit/c2d5dc705b4601be615b294a30ecbbc0aa984aaa))



## [v0.6.1] - 2026-08-10
### [0.6.1](https://github.com/Samuellct/Delivr/compare/v0.6.0...v0.6.1) (2026-08-10)


### Bug Fixes

* **delivery:** désactive Livré/Annulé après avoir marqué un cottage ciblé ([72b3870](https://github.com/Samuellct/Delivr/commit/72b3870c006daae8702a08aa38bee6cd4296b6bb))



## [v0.6.0] - 2026-08-10
## [0.6.0](https://github.com/Samuellct/Delivr/compare/v0.5.2...v0.6.0) (2026-08-10)


### Features

* **domain:** logique de navigation ciblée pour le mode Livraison ([2360177](https://github.com/Samuellct/Delivr/commit/2360177c9472a9fd45b01c688000900435947759))
* **list:** écran Liste avec statuts et navigation rapide ([4d67459](https://github.com/Samuellct/Delivr/commit/4d6745990ee0750a7c8d0b87af155f601f23f41a))



## [v0.5.2] - 2026-08-09
### [0.5.2](https://github.com/Samuellct/Delivr/compare/v0.5.1...v0.5.2) (2026-08-09)


### Bug Fixes

* **delivery:** agrandit le numéro de cottage et les boutons Livré/Annulé ([e869c22](https://github.com/Samuellct/Delivr/commit/e869c22ae46c552e0f23c0f94227108c8433ce97))



## [v0.5.1] - 2026-08-09
### [0.5.1](https://github.com/Samuellct/Delivr/compare/v0.5.0...v0.5.1) (2026-08-09)


### Documentation

* documente le mode Livraison dans le README ([e4ec63b](https://github.com/Samuellct/Delivr/commit/e4ec63b89c59b49fa43ab30e843ac5c9240b2382))



## [v0.5.0] - 2026-08-09
## [0.5.0](https://github.com/Samuellct/Delivr/compare/v0.4.0...v0.5.0) (2026-08-09)


### Features

* **delivery:** DeliveryViewModel pilote le mode Livraison ([c8ff34a](https://github.com/Samuellct/Delivr/commit/c8ff34ad814ace9d5d87fd9c3cfb6502963c3ec1))
* **delivery:** écran du mode Livraison, 4 icônes dans les coins ([bb54d2e](https://github.com/Samuellct/Delivr/commit/bb54d2eaa555c49464521dbe35d1dc13cd8e8f00))
* **domain:** logique pure de position en mode Livraison ([1251a04](https://github.com/Samuellct/Delivr/commit/1251a04713d34c5c2e8c2618f4c906cb3087aad2))
* **navigation:** démarre et reprend la tournée en mode Livraison ([c062528](https://github.com/Samuellct/Delivr/commit/c0625286651233ec653299551d3709084bd16b0e))
* **repository:** expose le statut par cottage et son écriture ciblée ([57a4f23](https://github.com/Samuellct/Delivr/commit/57a4f2375cfb3d398ea5d3c2382c19c5ee660eda))


### Tests

* **delivery:** tests Compose UI du mode Livraison ([d45986a](https://github.com/Samuellct/Delivr/commit/d45986a56cb71c80a05a9175f93e7c4bdecec57c))



## [v0.4.0] - 2026-08-08
## [0.4.0](https://github.com/Samuellct/Delivr/compare/v0.3.1...v0.4.0) (2026-08-08)


### Features

* **database:** schéma Room de la tournée en cours ([b8c0036](https://github.com/Samuellct/Delivr/commit/b8c0036c728151a75a3f5596874c952b358e3074))
* **home:** reprise réelle de la tournée en cours depuis Room ([8f8fa5d](https://github.com/Samuellct/Delivr/commit/8f8fa5d5c7c1f97ea40c3be7f634ba934abc5392))
* **repository:** RoundRepository au-dessus du DAO Room ([3bd30cb](https://github.com/Samuellct/Delivr/commit/3bd30cbe7a974c9ffa5a1eed5991132df378f2fa))
* **validation:** sauvegarde automatique de la tournée dans Room ([f39f347](https://github.com/Samuellct/Delivr/commit/f39f3473d0371bb2addf2c633f02e90adf136856))


### Documentation

* documente la persistance Room dans le README ([f5d5db1](https://github.com/Samuellct/Delivr/commit/f5d5db167805c0512c53f597bb41d46e2e0dd97a))


### Tests

* **database:** tests DAO Room et scénario de reprise ([4a2b1d7](https://github.com/Samuellct/Delivr/commit/4a2b1d770952ae1c455596ecf9dcc2336d40c048))


### Build Systems

* passer Kotlin en 2.2.21 et ajouter KSP + Room 2.8.4 ([7ff065d](https://github.com/Samuellct/Delivr/commit/7ff065dc598cbfef1ebea661482cc7c9601b542b))



## [v0.3.1] - 2026-08-08
### [0.3.1](https://github.com/Samuellct/Delivr/compare/v0.3.0...v0.3.1) (2026-08-08)


### Bug Fixes

* corrige un avertissement lint UnrememberedMutableState dans le test ([4a7e024](https://github.com/Samuellct/Delivr/commit/4a7e02433bcc11a3b2bf72e53f2dc5c87a34baed))



## [v0.3.0] - 2026-08-08
## [0.3.0](https://github.com/Samuellct/Delivr/compare/v0.2.1...v0.3.0) (2026-08-08)


### Features

* logique pure d'édition de la liste de cottages (Phase 4.2-4.5) ([6df2324](https://github.com/Samuellct/Delivr/commit/6df2324828e5ea4c13b176bf98933a641e673601))
* rend l'écran de validation éditable (Phase 4.2-4.5) ([37408c8](https://github.com/Samuellct/Delivr/commit/37408c8d02dc83e8538e01fb93550d217816d47c))


### Tests

* ajoute les tests Compose UI de l'écran de validation (Phase 4.6) ([088481c](https://github.com/Samuellct/Delivr/commit/088481cc384e0862d0c8307fa13d17c51462bdd7))



## [v0.2.1] - 2026-08-08
### [0.2.1](https://github.com/Samuellct/Delivr/compare/v0.2.0...v0.2.1) (2026-08-08)


### Bug Fixes

* corrige un chevauchement de glyphes dans la fixture + assouplit le test bout en bout (Phase 3.6) ([6bda54d](https://github.com/Samuellct/Delivr/commit/6bda54d6629cae109ae389c5db71701f46a985a8))



## [v0.2.0] - 2026-08-08
## [0.2.0](https://github.com/Samuellct/Delivr/compare/v0.1.0...v0.2.0) (2026-08-08)


### Features

* écran de validation minimal, en lecture seule (Phase 3) ([00a3ecf](https://github.com/Samuellct/Delivr/commit/00a3ecff8c3d243cdbf2ae2d616a12171ceca1ba))
* extraction pure des numéros de cottage (Phase 3.2-3.5) ([6641e87](https://github.com/Samuellct/Delivr/commit/6641e874999fa16927442b6393a8c4d50eb97ace))
* intègre ML Kit Text Recognition dans ocr/ (Phase 3.1) ([d94f1f9](https://github.com/Samuellct/Delivr/commit/d94f1f9f94e34ccf59274c8b831e3e701f9240e6))


### Tests

* pipeline OCR bout en bout sur la fixture + mise à jour ScanScreenTest (Phase 3.6) ([9abffef](https://github.com/Samuellct/Delivr/commit/9abffefd1f9e01bcdf087c1ada1b7c3a47e9a67b))


### Code Refactoring

* extrait la rotation EXIF et ajoute loadFullResolutionBitmap ([8464361](https://github.com/Samuellct/Delivr/commit/8464361ba663ea36680789d8b36409e3574c00ea))



## [v0.1.0] - 2026-08-07
## [0.1.0](https://github.com/Samuellct/Delivr/compare/v0.0.2...v0.1.0) (2026-08-07)


### Features

* ajoute la fixture synthétique de feuille de livraison (Phase 2.2) ([3eb54f6](https://github.com/Samuellct/Delivr/commit/3eb54f6b8174e2963cf5859c17cffaf51b692fdf))
* sonde ML Kit Text Recognition sur les échantillons (Phase 2.3) ([b419bb0](https://github.com/Samuellct/Delivr/commit/b419bb05511afab0e2ab1415fe08137fd7d1b790))



## [v0.0.2] - 2026-08-07
### [0.0.2](https://github.com/Samuellct/Delivr/compare/v0.0.1...v0.0.2) (2026-08-07)


### Bug Fixes

* applique les insets système (safeDrawingPadding) au niveau racine ([176914f](https://github.com/Samuellct/Delivr/commit/176914f2162f361cc213df561a2fffff33170205))
* désactive le dynamic color et corrige le flash blanc au démarrage ([ab13cc5](https://github.com/Samuellct/Delivr/commit/ab13cc5d218f1aa0ad26f6bf138e09b16176eac5))
* durabilité du scan (copie interne + SavedStateHandle) ([d67ff33](https://github.com/Samuellct/Delivr/commit/d67ff33f28ccb2527c349dcf920753b810add6a0))
* le scan ne relance plus la caméra après rotation ([3e4d4d1](https://github.com/Samuellct/Delivr/commit/3e4d4d10ceb99aed0d02303eb70a114e66679b52))
* sous-échantillonnage et orientation EXIF dans BitmapLoader ([88e3cb0](https://github.com/Samuellct/Delivr/commit/88e3cb09b75cd33a9a2ce8f9a07cd3360be198f7))
* supprime HomeViewModel, jamais instancié ([2932be0](https://github.com/Samuellct/Delivr/commit/2932be0cfe5e2872b63845c4d5fbdac054c68e4a))


### Tests

* ajoute les premiers tests réels et active ktlint ([4a6445d](https://github.com/Samuellct/Delivr/commit/4a6445dad351fa0ecce34cdee5a254f715ccfe41))


### Code Refactoring

* abstraction testable du lanceur de scanner ([cd411fb](https://github.com/Samuellct/Delivr/commit/cd411fb9a525dfcf25f71085a61e7abd93724b6e))
* erreurs de scan typées et externalisation des textes ([aaf03e2](https://github.com/Samuellct/Delivr/commit/aaf03e206a57e77ea55f0d1380fca69e61d652c1))



## [v0.0.1] - 2026-08-07
### [0.0.1](https://github.com/Samuellct/Delivr/compare/v0.0.0...v0.0.1) (2026-08-07)


# Changelog - Delivr
Toutes les modifications notables de cette application sont consignées ici.
