# Vérifier la persistance des observations réelles

Les dossiers sont enregistrés dans PostgreSQL et survivent au redémarrage de Java.
La base applicative a été nettoyée avec accord de l'utilisateur : trois observations Wikidata conservées,
aucune fixture. Les données fictives restent dans la base de tests.

1. Démarrer la base dédiée avec scripts/Start-Database.ps1 si nécessaire.
2. Recharger Maven puis lancer clean package : 109 tests réussis attendus.
3. Arrêter et relancer StudentCareerApplication dans IntelliJ, avec le même réglage temporaire Java qu'avant.
4. Envoyer GET http://localhost:8080/api/status : storage POSTGRESQL.
5. Envoyer GET http://localhost:8080/api/companies : noter totalElements et un id.
6. Arrêter Java et le relancer. Sans nouvel import, refaire le GET : mêmes dossiers, identifiants et sources.
7. Envoyer GET http://localhost:8080/api/companies?q=emtel : WEB_OBSERVATION, UNVERIFIED.
8. Envoyer POST http://localhost:8080/api/imports/demo : 404 attendu, car les fixtures ne sont plus importables par défaut.

La collection postman/Persistence.postman_collection.json mémorise les informations automatiquement.
Exécuter 01 à 03, arrêter/relancer Java, puis 04 à 06. Le dernier POST est volontairement refusé.
Elle n'effectue aucune collecte externe et ne crée pas de données fictives.

Les tests automatisés activent explicitement la démo dans student_career_test.
Une démonstration manuelle peut être activée avec --app.demo.enabled=true, sur une base dédiée à cet usage.
Cette activation n'est pas nécessaire au parcours de consultation réelle.
Le nettoyage scripts/Remove-FictionalData.ps1 est manuel, ciblé et transactionnel ; il ne s'exécute pas au démarrage.

Le port de PostgreSQL est 15432 ; Postman utilise le port de Java, 8080.
Après arrêt de Java, Stop-Database.ps1 arrête la seule instance PostgreSQL du projet ; les données restent sur disque.
Le lanceur Start-Api.ps1 redémarre cette base si nécessaire. Aucun mot de passe n'est à envoyer dans le chat.
