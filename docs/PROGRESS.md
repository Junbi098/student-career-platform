# Suivi du projet

## Méthode

Avant chaque semaine : rappeler objectifs, résultat attendu et notions abordées.
Développer la semaine, puis checkpoint avec changements, tests et procédure locale.
Chaque commit et chaque push exigent une validation explicite de l'utilisateur.
Ne pas démarrer la semaine suivante avant le checkpoint.

## Semaine 1 — validée et publiée

- POC HTML local vers dossier JSON, cas limites et première correction de provenance.
- 44 tests réussis ; démonstration exécutée ; validation IntelliJ confirmée par l'utilisateur.
- Commit b8b2caa poussé sur main après accord explicite.

## Semaine 2 — validée et publiée

- Lecture avec provenance, date issue du lecteur et catégorie structurée.
- Normalisation Unicode et espaces ; comparaison prudente des doublons.
- Traitement par lot : erreurs isolées, ordre et toutes les provenances conservés.
- Bilan JSON, fixtures multiples et démonstration de répertoire local.
- Compilation complète : 62 tests réussis, aucun échec, erreur ou test ignoré.
- Démos individuelle, mixte et répertoire exécutées et sorties JSON vérifiées.
- Aucun nouveau composant installé, aucune configuration globale Git ou Java changée.
- Validation locale IntelliJ confirmée ; commit c35a566 poussé sur main après accord explicite.

## Semaine 3 — validée et publiée

- API Spring Boot, catalogue en mémoire, recherche, filtres et pagination.
- Import explicite de quatre fixtures intégrées, provenance classpath et doublons conservés.
- Réimport sans ajout de dossiers, erreurs HTTP structurées et script de vérification locale.
- Procédure de démarrage IntelliJ et limites documentées dans README et docs/WEEK_3.md.
- Compilation complète et programme exécutable : BUILD SUCCESS, 78 tests réussis, aucun échec, erreur ou test ignoré.
- JAR exécuté avec son import intégré : catalogue vide initial, filtres, pagination, réimport et erreurs vérifiés.
- Lanceur Start-Api.ps1 et scénario Test-Api.ps1 exécutés ; catalogue vide après redémarrage confirmé.
- Démos historiques individuelle et par lot réexécutées avec succès.
- Serveurs temporaires de vérification arrêtés ; aucun réglage global Java, Windows ou Git modifié.
- Aucun commit ni push de cette semaine effectué.

## Refactoring pédagogique — 5 novembre 2026

- Réorganisation en company (métier), source (lecture), web (API) et common (erreur partagée).
- Classes renommées selon leur rôle ; correspondance détaillée dans docs/ARCHITECTURE.md.
- Main initial, test d'addition et wrapper CompanyNameExtractor retirés ; cas redondants retirés.
- Fixtures HTML partagées, anciens dossiers devenus vides supprimés ; exemples d'export conservés et renommés.
- Coordonnées Maven : com.studentcareer:student-career-platform:1.0-SNAPSHOT ; lanceur adapté.
- Vérification finale : clean package, 73 tests réussis, zéro échec, erreur ou test ignoré.
- Les 5 tests retirés ne couvraient pas de comportement métier distinct du parseur conservé.
- Collection Postman : 11 requêtes et 17 assertions exécutées contre le JAR local via un vérificateur HTTP/JavaScript.
  La collection n'a pas été importée dans l'interface Postman pendant cette vérification.
- Script Test-Api.ps1 et exemples SingleCompanyExportExample / BatchCompanyExportExample exécutés avec succès.
- Guide débutant dans docs/POSTMAN_GUIDE.md ; serveurs temporaires arrêtés.
- Parcours Postman validé par l’utilisateur : disponibilité, import, consultation, détail, recherche, filtres, pagination, erreurs 400/404 et réimport à total constant.
- Parcours Postman validé ; commit 4521285 poussé sur main après accord explicite.

## Complément de la semaine 2 — collecte réelle initiale, validée et publiée

- Écart de parcours reconnu : la semaine 2 livrée précédemment traitait seulement les fichiers locaux.
- Sources mauriciennes repérées ; comparaison et décisions dans docs/SOURCES.md.
- Portail Open Data non retenu pour l'automatisation à cause des règles d'accès actuelles.
- Collecteur initial de trois fiches Wikidata connues, données CC0, lecture explicite et limitée.
- Pas de recherche autonome générale sur Internet, pas de collecte d'offres ou de preuve de recrutement.
- Parser JSON séparé, normalisation et traitement par lot existants réutilisés.
- Traçabilité enrichie : URL lue, date de lecture, date annoncée par la source, licence et statut UNVERIFIED.
- Délais, taille maximale, contrôle de l'identité, réponses 429/503 et refus d'accès testés localement.
- Compilation et suite complète : 100 tests réussis, zéro échec, erreur ou test ignoré.
- Vérification réelle du 5 novembre : 3 dossiers importés, aucune erreur ; réimport immédiat refusé en 429.
- Catalogue mixte vérifié : 3 observations Web et 3 dossiers fictifs distincts.
- Parcours fictif et ses 11 requêtes/17 assertions vérifiés dans ce catalogue mixte.
- Collections Postman et guides adaptés.
- Validation Postman utilisateur confirmée : sources, collecte, dossier Emtel, six dossiers mixtes et protection HTTP 429.
- Commit b2279ed poussé sur main après validation Postman et accord explicite.
- Le statut métier des entreprises reste non vérifié ; secteur et ville ne sont pas devinés.
- État historique à ce checkpoint : catalogue en mémoire ; persistance traitée à la semaine 4.

## Semaine 4 — validée localement

- PostgreSQL 17 existant réutilisé pour une instance dédiée au projet, port 15432, sans modifier les services 16/17.
- Bases student_career et student_career_test séparées ; compte applicatif non superutilisateur.
- Identifiants générés et données sous .local, exclu de Git ; scripts d'initialisation/démarrage/arrêt fournis.
- Contrat CompanyRepository et adaptateur Spring Data JPA ; double en mémoire réservé aux tests unitaires.
- Flyway V1 crée les dossiers et les sources ; Hibernate valide le schéma, sans création automatique destructive.
- Provenance, licences, catégories et dates sauvegardées ; vérification indépendante inconnue, verifiedAt reste null.
- Transactions, rollback, ordre stable et réimport sans ajout de lignes vérifiés sur PostgreSQL.
- Bases de test contrôlées avant Flyway/JPA puis avant nettoyage, pour protéger la base applicative.
- clean package : 108 tests réussis, zéro échec, erreur ou test ignoré. Aucune collecte Wikidata durant cette semaine.
- JAR vérifié : import, arrêt de Java et du cluster dédié, redémarrage, lecture des mêmes identifiants et sources sans import.
- Collection de persistance exécutée en deux phases, 6 requêtes et 12 assertions ; script API existant également validé.
- Trois dossiers fictifs de vérification conservés dans la base applicative ; aucun ancien dossier mémoire transféré automatiquement.
- Documentation et parcours Postman dans docs/WEEK_4.md et docs/PERSISTENCE_GUIDE.md.
- Aucun commit ni push de la semaine 4 effectué.

## Prochaine action

Publier le checkpoint autorisé et vérifier la synchronisation Git.
Rappeler les objectifs de la semaine 5 avant de la commencer.


## Nettoyage des données fictives — autorisé par l'utilisateur

- Persistance des observations Web et réimport à total 6 validés par l'utilisateur dans Postman.
- Suppression transactionnelle des seuls dossiers FIXTURE et de leurs sources dans student_career.
- Empreinte du contenu non fictif comparée avant/après dans la transaction : inchangée.
- Résultat du nettoyage : 3 dossiers WEB_OBSERVATION, 0 dossier FIXTURE.
- Import de démo et service associés désactivés par défaut, activables explicitement par app.demo.enabled.
- Tests d'intégration activant la démo uniquement dans leur base dédiée ; test du refus 404 ajouté.
- Script Test-Api.ps1 adapté : lecture seule par défaut, mode fictif explicite avec -IncludeDemo.
- Bilan final : 109 tests réussis, zéro échec, erreur ou test ignoré.
- Programme vérifié après nettoyage : 3 observations Web, aucune fixture, route de démo en 404 et script de lecture seule validé.
- Java temporaire de vérification arrêté ; redémarrage utilisateur nécessaire pour appliquer la désactivation à son ancien processus.
- Persistance des observations réelles, réimport et nettoyage validés par l’utilisateur.
- Commit et push de la semaine 4, nettoyage inclus, explicitement autorisés.

## Checkpoint phase 2 — semaines 5, 6 et 7

- Semaine 4 publiée : commit f1f5845, après autorisation utilisateur.
- Semaine 5 : modèle stages/emplois juniors, association à une entreprise, critères explicites, CRUD sans suppression et persistance Flyway V2.
- Semaine 6 : détection JSON-LD JobPosting dans du HTML fourni, réimport sans doublon, changement invalidant la confirmation, historique, fermeture, expiration et ancienneté de vérification.
- Semaine 7 : recherche par texte, domaine, lieu, type, mode de travail, éligibilité et statut ; classement explicable et pagination.
- 148 tests JUnit réussis, aucun échec ni erreur. Parcours HTTP de la collection : 25 requêtes, 43 assertions réussies sur student_career_test.
- Exemples fictifs uniquement dans la base de test ; aucune collecte d'annonce réelle et aucun robot général de recherche Web.
- L'éligibilité inconnue reste inconnue ; une localisation ne prouve pas un droit au travail. La confirmation repose sur une déclaration manuelle et une référence de source.
- Recherche en mémoire adaptée au catalogue initial ; pas encore de collecte planifiée, authentification ou contrôle des rôles (phases suivantes).
- Parcours exécuté directement dans Postman : 25 requêtes, 43 assertions réussies, zéro échec et zéro erreur.
- Résultats présentés à l’utilisateur ; commit et push de la phase 2 explicitement autorisés.

Prochaine étape : rappeler les objectifs de la semaine 8 avant de commencer le frontend React.

- Redémarrage du JAR vérifié sur la base de test : mêmes identifiants, version, statut, cinq événements de vérification et origine HTML conservés. Serveurs temporaires arrêtés.
