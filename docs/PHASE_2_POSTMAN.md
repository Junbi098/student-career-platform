# Tester toute la phase 2 sans polluer le catalogue réel

## Deux environnements distincts

- API normale : http://localhost:8080, base student_career, vraies observations.
- API d'exercices : http://localhost:8081, base student_career_test, exemples fictifs autorisés.

Ne pas créer les exemples ci-dessous sur l'API normale. Le lanceur de test force l'URL de la base de test.
La collection Postman vérifie /api/status avant chaque requête et refuse de l'envoyer si la base n'est
pas student_career_test. Les tests Maven utilisent aussi cette base : arrêter l'API d'exercices pendant
une exécution Maven qui nettoie les données de test.

## 1 — Préparer le programme

Dans IntelliJ, recharger Maven et exécuter clean package. La migration V2 ajoute les tables des opportunités
sans remplacer V1 ni supprimer les entreprises existantes. Les tests restent dans la base de test.

## 2 — Lancer l'API d'exercices

Dans le terminal PowerShell du projet :

```powershell
.\scripts\Start-TestApi.ps1
```

Ce lanceur utilise student_career_test, le port 8081 et active la démo uniquement pour cette instance.
Il laisse l'API normale sur 8080 et son catalogue séparés. Laisser le terminal ouvert pendant Postman.
Si le port est occupé, arrêter votre ancienne instance d'exercices ou choisir -Port 8082 puis adapter baseUrl.

## 3 — Vérifier la base avant d'écrire

Créer GET http://localhost:8081/api/status. Vous devez voir database student_career_test.
Le nom de la base est maintenant retourné par /api/status pour distinguer clairement les environnements.
Si vous voyez student_career, ne pas créer d'exemples et revenir à l'étape 2.

## 4 — Importer la collection

Importer postman/Phase-2-Test.postman_collection.json dans Postman.
La collection s'appelle PHASE 2 - TESTS ISOLÉS UNIQUEMENT. Exécuter les requêtes dans l'ordre.
Les identifiants, versions et corps de mise à jour sont mémorisés automatiquement.
Un jeton différent est généré à chaque parcours pour ne pas confondre les exercices antérieurs.
Aucun site d'emploi ou d'entreprise n'est contacté.

La protection de collection utilise les fonctions officielles pm.sendRequest et pm.execution.skipRequest :
https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-send-request
https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution

## Les étapes et leur résultat

01 à 03 : vérifier la base et charger/choisir une entreprise fictive du catalogue de test.
04 : créer un stage synthétique, réponse 201, status UNVERIFIED, sourceFetchedByServer false.
05 : consulter son dossier et sa provenance déclarée.
06 : filtre INTERNATIONAL strict : zéro résultat car l'éligibilité est inconnue.
07 : même filtre avec includeUnknownEligibility=true : le dossier apparaît comme UNKNOWN.
08 : confirmation sans déclaration de consultation : 400 attendu.
09 : simuler une vérification dans la base de test : OPEN.
10 : verifiedOnly=true et recherche de localisation sans accent : un résultat.
11 : modifier le titre : retour UNVERIFIED et lastVerifiedAt null.
12 : retrouver l'historique de soumission, confirmation et modification.
13 à 16 : fermer, masquer des résultats actifs, consulter les fermées et rouvrir sans confirmation automatique.
17 : recréer le même stage : 409 attendu, pas de ligne supplémentaire.
18 à 20 : détecter un JobPosting dans du HTML fourni, réimporter sans doublon et lire la provenance.
21 à 23 : enregistrer une ancienne annonce, la retrouver comme EXPIRED et refuser sa confirmation.
24 : taille de page 101 : 400 attendu.
25 : identifiant inexistant : 404 attendu.

Les réponses 400/404/409 de ce parcours sont volontaires. Elles ne signifient pas une panne.
L'historique et l'expiration utilisent les informations du dossier ; aucun recrutement réel n'est déclaré.

## Corps d'une soumission manuelle

Ce modèle est pour la base de test. Remplacer companyId par l'id choisi dans la requête 03 :

```json
{
  "companyId": "UUID_DU_DOSSIER_DE_TEST",
  "title": "Stage Java fictif",
  "type": "INTERNSHIP",
  "description": "Annonce synthétique destinée uniquement à la validation isolée.",
  "sector": "Software",
  "location": "Ébène, Mauritius",
  "workMode": "HYBRID",
  "publishedOn": "2020-01-01",
  "deadline": "2099-12-31",
  "sourceUrl": "https://example.test/jobs/test-stage",
  "sourceTitle": "Source synthétique pour exercice",
  "externalReference": "TEST-STAGE",
  "mauritianEligibility": "UNKNOWN",
  "internationalEligibility": "UNKNOWN"
}
```

Les dates lointaines sont volontaires pour cet exercice, pas des offres publiées.
Un vrai opérateur ne saisira dans la base normale que les informations réellement présentes dans une source.
La saisie ne confirme jamais automatiquement l'annonce.

## Confirmer, modifier, fermer

Une confirmation a cette forme (version doit venir du dernier GET) :

```json
{
  "version": 0,
  "action": "CONFIRM",
  "sourceReviewed": true,
  "evidenceUrl": "https://example.test/jobs/test-stage",
  "notes": "Vérification simulée uniquement dans la base de test"
}
```

Pour CLOSE ou REOPEN, sourceReviewed et evidenceUrl sont facultatifs ; fournir version, action et notes.
PUT attend un objet avec version et opportunity, contenant tous les champs de la soumission.
Après une modification, relire la nouvelle version avant l'action suivante. Une version ancienne donne 409.

## Recherche

Exemples sur 8081 :

- /api/opportunities?q=java&sector=software&location=ebene
- /api/opportunities?type=INTERNSHIP&workMode=HYBRID
- /api/opportunities?studentType=INTERNATIONAL&includeUnknownEligibility=true
- /api/opportunities?studentType=MAURITIAN&verifiedOnly=true
- /api/opportunities?status=EXPIRED
- /api/opportunities?includeInactive=true&sort=CLOSING_SOON&page=0&size=10

Les filtres d'éligibilité sont des indications déclarées, pas une décision sur le droit de travailler.
L'inclusion explicite des inconnus sert à explorer sans les présenter comme acceptés.

## Vérifier la sauvegarde

Après le parcours, arrêter l'API d'exercices avec Ctrl+C puis relancer Start-TestApi.ps1.
Le GET du dossier par son id doit encore fonctionner tant que Maven n'a pas nettoyé student_career_test.
La base normale conserve ses observations réelles indépendamment de ces exercices.

À la fin, transmettre le numéro de la première requête qui échoue ou confirmer le parcours complet.
Aucun commit ni push n'est réalisé sans validation explicite.
