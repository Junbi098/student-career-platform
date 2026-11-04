# Student Career Intelligence Platform

POC Java 21 pour organiser des informations sur les entreprises utiles aux etudiants a Maurice.
Les donnees de demonstration sont fictives : aucune entreprise n'est declaree recruteuse.

## Fonctionnement

Fichier HTML local -> LocalFileSourceFetcher -> CompanyIntelligenceService
-> CompanyHtmlParser -> CompanyDossierPipeline -> CompanyDossierJsonWriter.

- Java 21, Maven, jsoup 1.23.2, JUnit Jupiter 5.12.2, Surefire 3.5.3, Jackson 2.22.4.
- Le nom est obligatoire ; les autres informations peuvent etre inconnues (null).
- Le premier element correspondant a chaque selecteur est utilise.
- Un premier nom vide signifie absence d'entreprise, meme si un suivant est rempli.
- Un site web doit etre une URL HTTP(S) absolue avec un hote ; sinon il reste inconnu.
- Le HTML incomplet est analyse par jsoup ; cela ne garantit pas que ses informations sont vraies.
- Les erreurs de lecture remontent comme IOException ; une entreprise absente donne Optional.empty().

## Tester dans IntelliJ

1. Ouvrir ce dossier comme projet Maven et selectionner le JDK 21.
2. Dans la fenetre Maven, executer Lifecycle > test (ou `mvn test` si Maven est dans le PATH).
3. Verifier BUILD SUCCESS et l'absence d'echecs. Les rapports sont dans target/surefire-reports.
4. Ouvrir src/test/java/com/studentcareer/poc/demo/CompanyDossierDemo.java.
5. Lancer sa methode main avec IntelliJ, avec le dossier du projet comme repertoire de travail.
6. Verifier le JSON affiche et target/demo-output/company-dossier.json.
7. La source doit etre une URI file: vers la fixture, avec un titre explicitement fictif.

Maven integre a IntelliJ peut etre utilise sans modifier le PATH.
Les fichiers target/ et .idea/ restent exclus de Git.

## Provenance et limites

SourceReference accepte les fichiers locaux absolus et les URL HTTP(S) avec hote.
Le pipeline exige la meme URI normalisee pour la lecture et la reference : une URL web
independante ne peut plus etre presentee comme la source d'un fichier local.
La date de consultation reste fournie par l'appelant ; la demo utilise Instant.now().
Le titre signale les donnees fictives, mais aucun statut de verification structure n'existe encore.
Une source n'est pas une preuve de recrutement, ni une validation automatique du contenu.
Il n'existe encore aucun lecteur web : aucun site reel n'est consulte.

Avant les sources reelles : faire produire URI effective et date par le lecteur dans un resultat
de lecture, distinguer explicitement fixture et observation, et definir les sources autorisees.

## Suite

Semaine 1 : POC local, cas limites, provenance et documentation.
Semaine 2 proposee : collecte structuree, normalisation et doublons, apres validation du checkpoint.
Spring Boot, PostgreSQL et React viendront dans les phases suivantes.
