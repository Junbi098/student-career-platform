# Semaine 3 — première API REST

> Note historique : les noms de classes et le nombre de tests ci-dessous décrivent le premier état de la semaine 3.
> Après le refactoring demandé, consulter ARCHITECTURE.md et POSTMAN_GUIDE.md. Le bilan actuel est de 73 tests réussis.

## Objectif et périmètre

Rendre le moteur Java accessible via HTTP, consulter les dossiers, chercher et filtrer,
avec une gestion claire des erreurs. Catalogue en mémoire, import fictif explicite et sources conservées.
Pas de base de données, de comptes utilisateurs, de frontend ou de collecte web réelle à cette étape.

## Architecture

```text
Requête HTTP -> Controller -> Service -> Catalogue en mémoire -> DTO -> JSON HTTP
                                |
Import de démonstration -> CompanyBatchService -> moteur de parsing/normalisation existant
                                |
                  ClasspathFixtureSourceFetcher -> fixtures intégrées + provenance
```

- `StudentCareerApplication` démarre Spring Boot ; l'ancien Main reste un exemple historique.
- Les contrôleurs traduisent les paramètres HTTP et délèguent au service.
- Le service de catalogue applique les filtres et la pagination.
- Le dépôt conserve un dossier par catégorie et URI de source ; ses opérations sont synchronisées.
- Les DTO exposent explicitement les données HTTP, sans modifier le modèle métier du POC.
- `ApiExceptionHandler` fournit les erreurs métier 400/404 et un message générique pour les erreurs imprévues.
- Le lecteur de fixtures accepte uniquement quatre ressources connues ; aucun chemin reçu par HTTP n'est lu.
- Un réimport conserve l'identifiant du dossier associé à la source et actualise sa provenance.
- Les doublons possibles restent des dossiers distincts. Le catalogue ne prétend pas résoudre les identités.

## Notions Java / Spring

Un contrôleur REST est un point d'entrée HTTP. `@GetMapping` associe une méthode Java à une route
consultable ; `@PostMapping` représente ici l'action explicite d'importer la démonstration.

Un service concentre les règles du traitement. Spring construit les composants annotés
et fournit les dépendances via leurs constructeurs : c'est l'injection de dépendances.
Le moteur du POC reste utilisable sans démarrer Spring, notamment par les démos historiques.

Un DTO décrit le format de la réponse. Les records Java permettent d'exprimer ces données simplement.
L'identifiant UUID sert à consulter un dossier sans utiliser son nom ou son chemin comme URL d'API.
Il est déterministe à partir de la catégorie et de l'URI de source : ce n'est pas un identifiant métier d'entreprise.

La pagination limite le nombre de dossiers renvoyés ; elle ne signifie pas encore une requête SQL.
Les totaux comptent les dossiers, dont les doublons possibles, et non les entreprises uniques.

`ProblemDetail` est un format JSON d'erreur avec `status`, `title`, `detail` et `instance`.
Une recherche sans résultat retourne normalement 200 et une liste vide, pas 404.

## Maven et compatibilité

Le parent Spring Boot 4.1.2 gère Spring, Tomcat, les plugins et les tests :
JUnit Jupiter 6.0.3 et Surefire 3.5.6 remplacent les versions explicites de la semaine 2.
Les tests continuent d'utiliser les imports `org.junit.jupiter` et les 62 tests antérieurs sont conservés.
Java reste en version 21. Les coordonnées Maven historiques restent inchangées.
Le plugin Boot précise la classe de démarrage, car le projet conserve son ancien Main.

La couche HTTP utilise Jackson 3, fourni par Boot. Les exports JSON du POC conservent Jackson 2.22.4
avec son BOM cohérent, afin de ne pas mélanger les versions core/databind dans cette famille.
Les deux familles ont des packages distincts ; les tests valident les exports existants et les réponses HTTP.

Les tests HTTP démarrent un vrai serveur sur un port aléatoire et utilisent le client HTTP du JDK.
Le chemin des sockets temporaires Java est dirigé vers `target/` pour éviter l'erreur de chemin Windows
abrégé observée pendant l'audit. Ce réglage concerne les JVM de test, sans modification globale.

## Validation à reproduire

Suivre le README : `clean test`, lancer StudentCareerApplication, puis scripts/Test-Api.ps1.
Vérifier le catalogue vide initial, le POST de démonstration, les 3 dossiers, les filtres, le détail,
la pagination, la stabilité du nombre de dossiers après réimport et les erreurs 400/404.
Arrêter puis relancer l'application : le catalogue redevient vide, conformément au stockage en mémoire.

## Limites et suite

- Aucun enregistrement sur disque ou en base pour le catalogue : la persistance sera une étape distincte.
- Aucun compte ni authentification ; écoute locale uniquement et aucun import de chemin arbitraire.
- L'import fictif n'est pas une preuve de recrutement ou une observation web réelle.
- Les fixtures intégrées sont volontairement accessibles dans un JAR, via des identifiants classpath.
- Il n'existe pas encore de rapprochement entre les importations de sources réelles.
- L'état UP indique seulement que le processus répond ; aucun service externe n'est vérifié.

Références officielles consultées :
- https://docs.spring.io/spring-boot/system-requirements.html
- https://docs.spring.io/spring-boot/reference/using/build-systems.html
- https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html


## Résultats obtenus avant checkpoint

- `clean package` : BUILD SUCCESS ; 78 tests, zéro échec, zéro erreur, zéro test ignoré.
- Démarrage du JAR avec l'option temporaire Java documentée et import des ressources classpath validés.
- Scripts Start-Api.ps1 et Test-Api.ps1 exécutés sur un port libre.
- Redémarrage : catalogue vide confirmé ; import et requêtes de nouveau fonctionnels.
- Anciennes démonstrations individuelle et par lot exécutées ; bilan mixte de six entrées inchangé.
- Serveurs temporaires arrêtés. Aucun commit ou push de cette semaine effectué.
- Avertissement non bloquant de chargement dynamique de l'agent Mockito pendant les tests Spring.
- Validation utilisateur dans IntelliJ encore attendue avant autorisation de commit et push.
