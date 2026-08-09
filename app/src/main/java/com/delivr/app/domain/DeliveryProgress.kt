package com.delivr.app.domain

/**
 * Un cottage de la tournée avec son statut courant. Le domaine ne
 * manipulait jusqu'ici que des `List<Int>` (`CottageList.kt`, Phase 4) :
 * suffisant tant que la seule question était « quels numéros, dans quel
 * ordre ». Le mode Livraison (Phase 6) a besoin du couple numéro + statut
 * pour savoir où en est la tournée, sans pour autant remonter les entités
 * Room (`database/CottageEntity.kt`) jusqu'à l'UI — d'où ce type de
 * domaine, que `repository/RoundRepository.kt` produit à la relecture.
 *
 * Pas de `position` ici : c'est l'index dans la `List<Cottage>` qui la
 * porte, exactement comme `RoundDao.selectCottages()` la garantit par son
 * `ORDER BY position ASC`.
 */
data class Cottage(
    val number: Int,
    val status: CottageStatus = CottageStatus.A_FAIRE,
)

/**
 * Index du cottage « courant » en mode Livraison : le **premier** de la
 * tournée encore [CottageStatus.A_FAIRE].
 *
 * Décision de la Phase 6 : la position courante est **dérivée** des statuts,
 * jamais stockée. Aucune colonne « curseur » n'est donc ajoutée au schéma
 * Room (donc aucune migration), et il ne peut pas exister d'incohérence
 * entre un curseur persisté et les statuts persistés — même raisonnement
 * que `HomeViewModel`, qui recalcule « existe-t-il une tournée » plutôt que
 * de mémoriser un booléen qui pourrait devenir faux.
 *
 * Rend `cottages.size` quand plus rien n'est à faire (tournée terminée) et
 * quand la liste est vide : un seul cas « fin de tournée » à traiter en
 * aval, testé par `DeliveryProgressTest`.
 *
 * Cas non monotone assumé (statuts « à trous », par ex. cottage 1 à faire
 * et cottage 2 déjà livré, possible via une future navigation rapide en
 * Phase 7) : la règle « le premier à faire » reste alors la seule
 * définition sensée du cottage courant.
 */
fun currentCottageIndex(cottages: List<Cottage>): Int {
    val index = cottages.indexOfFirst { it.status == CottageStatus.A_FAIRE }
    return if (index < 0) cottages.size else index
}

/**
 * Applique [status] au cottage courant (tap sur Livré, appui long sur
 * Annulé — `TODO_V1.md` 6.3). Rend [cottages] inchangée si la tournée est
 * déjà terminée : l'écran grise déjà les deux icônes dans ce cas, mais la
 * fonction reste sûre indépendamment de son appelant (même garde-fou que
 * `addCottageNumber` dans `CottageList.kt`).
 *
 * Avancer d'une position n'est pas une opération distincte : passer le
 * courant à LIVRE ou ANNULE fait mécaniquement du suivant le nouveau
 * courant, par [currentCottageIndex].
 */
fun markCurrentCottage(
    cottages: List<Cottage>,
    status: CottageStatus,
): List<Cottage> {
    val index = currentCottageIndex(cottages)
    if (index >= cottages.size) return cottages
    return cottages.mapIndexed { i, cottage -> if (i == index) cottage.copy(status = status) else cottage }
}

/**
 * « Retour » (coin haut-gauche, `TODO_V1.md` 6.2) : repasse le cottage
 * **précédent** à [CottageStatus.A_FAIRE], ce qui en refait le courant par
 * [currentCottageIndex]. Il n'y a donc pas de pointeur à déplacer — corriger
 * une erreur de saisie et reculer sont exactement le même geste.
 *
 * Sans effet si le courant est déjà le premier cottage (on ne peut pas
 * reculer avant le début de la tournée) ou si la liste est vide. Quand la
 * tournée est terminée, l'index courant vaut `size` : la cible est alors le
 * dernier cottage, ce qui permet de rattraper le tout dernier statut.
 */
fun goBackToPreviousCottage(cottages: List<Cottage>): List<Cottage> {
    val target = currentCottageIndex(cottages) - 1
    if (target < 0) return cottages
    return cottages.mapIndexed { i, cottage ->
        if (i == target) cottage.copy(status = CottageStatus.A_FAIRE) else cottage
    }
}
