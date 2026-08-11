# Règles ProGuard/R8 spécifiques au projet Delivr.
# Voir : https://developer.android.com/build/shrink-code
#
# Volontairement vide (Phase 8.2, TODO_V1.md) : Room, Navigation Compose et
# les composants Play services / ML Kit utilisés ici embarquent chacun leurs
# propres règles consommateur dans leur AAR (fusionnées automatiquement par
# AGP), et Compose fait de même. Une règle -keep n'est ajoutée ici que si un
# smoke test sur l'APK release minifié révèle un besoin réel — jamais de
# façon préventive, sinon ça annule silencieusement le bénéfice de R8.
