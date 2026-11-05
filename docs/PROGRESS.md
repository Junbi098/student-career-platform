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

## Semaine 3 — validée localement

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
- Commit et push de la semaine 3 et du refactoring explicitement autorisés par l’utilisateur.

## Prochaine action

Publier le checkpoint autorisé et vérifier la synchronisation Git.
Rappeler les objectifs de la semaine suivante avant de la commencer.
