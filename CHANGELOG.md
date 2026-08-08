
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
