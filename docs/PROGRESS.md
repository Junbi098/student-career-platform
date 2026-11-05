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

## Complément de la semaine 2 — collecte réelle initiale, validée localement

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
- Commit et push du complément explicitement autorisés par l’utilisateur.
- Le statut métier des entreprises reste non vérifié ; secteur et ville ne sont pas devinés.
- Le catalogue reste en mémoire ; semaine 4 non commencée.

## Prochaine action

Publier le checkpoint autorisé et vérifier la synchronisation Git.
La découverte automatique élargie et le recoupement des données restent des travaux futurs explicites.
Rappeler les objectifs de la semaine 4 avant toute persistance.
