# Student Career Platform

Une application Java qui transforme des pages HTML en dossiers d'entreprises et permet de consulter ces dossiers avec une API.
Pour le moment, toutes les données de démonstration sont fictives. L'application ne recherche pas encore de stages sur Internet.

## Commencer ici

1. Ouvrir ce dossier dans IntelliJ et recharger Maven.
2. Exécuter le goal Maven `clean package`. Résultat attendu : **73 tests réussis**, `BUILD SUCCESS`.
3. Dans le terminal PowerShell d'IntelliJ, lancer :

```powershell
.\scripts\Start-Api.ps1
```

4. Laisser ce terminal ouvert. Le serveur écoute sur `http://127.0.0.1:8080`.
5. Suivre le [guide Postman, étape par étape](docs/POSTMAN_GUIDE.md).

Le lanceur applique uniquement à ce processus le réglage temporaire nécessaire à Java 21 sur ce poste Windows.
Si les scripts PowerShell sont désactivés, la commande équivalente depuis la racine est :

```powershell
java -Djdk.net.unixdomain.tmpdir=target -jar target/student-career-platform-1.0-SNAPSHOT.jar
```

Pour démarrer avec le bouton Run d'IntelliJ, lancer `StudentCareerApplication.main()` et mettre dans les **VM options** :
`-Djdk.net.unixdomain.tmpdir=D:/Travail/06_Projets_Java/student-career-platform/target`.
Le dossier target est créé par Maven. Aucun réglage global Windows ou Java n'est nécessaire.

## Comprendre le projet

- [Explication de l'API et exercices Postman](docs/POSTMAN_GUIDE.md)
- [Organisation du code et noms des classes](docs/ARCHITECTURE.md)
- [Suivi du développement](docs/PROGRESS.md)

## Ce que vous devez obtenir

Le catalogue est vide au démarrage. Une requête POST charge quatre fichiers HTML fictifs.
Deux entreprises sont reconnues, une seconde source correspond à un doublon possible et un fichier ne contient pas de nom.
L'application conserve **trois dossiers pour deux entreprises fictives distinctes**, avec leurs sources.
Vous pouvez ensuite les consulter et les rechercher avec des requêtes GET.
Arrêter le serveur efface ce catalogue temporaire. Ctrl+C arrête le lanceur.

## Tests et outils

Java 21, Spring Boot 4.1.2, Maven, jsoup 1.23.2, JUnit Jupiter 6.0.3 et Surefire 3.5.6.
Les exports existants utilisent Jackson 2.22.4 ; Spring Boot utilise Jackson 3 pour les réponses HTTP.
Les 73 tests couvrent le parsing, les sources, les exports, les lots, les recherches et les requêtes HTTP.
Cinq anciens tests ont été retirés : une addition de démonstration et quatre cas d'un ancien extracteur
qui répétaient ceux du parseur conservé. Aucune vérification métier distincte n'a été supprimée.

Les deux exemples d'export restent exécutables dans IntelliJ sous `src/test/java/com/studentcareer/examples` :
`SingleCompanyExportExample` et `BatchCompanyExportExample`. Ils écrivent dans `target/demo-output/`.
Les fixtures partagées par l'API et les tests sont dans `src/main/resources/demo/`.

Vérification PowerShell complémentaire, une fois le serveur démarré :

```powershell
.\scripts\Test-Api.ps1
```

Si le port 8080 est occupé, lancer `Start-Api.ps1 -Port 8081`, modifier `baseUrl` dans Postman
et utiliser `Test-Api.ps1 -BaseUrl http://127.0.0.1:8081`.
Les avertissements Mockito pendant les tests sont non bloquants ; vérifier le bilan Maven final.
Les notes WEEK_2.md et WEEK_3.md décrivent l'historique : leurs anciens noms de classes ne sont plus les noms actuels.
Les dossiers générés target/ et les réglages personnels .idea/ restent exclus de Git.
