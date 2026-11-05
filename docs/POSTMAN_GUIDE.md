> Ce guide décrit le parcours fictif de démonstration. L'import /api/imports/demo est maintenant
> désactivé par défaut : utiliser REAL_COLLECTION_GUIDE.md pour les données réelles et GET /api/companies
> pour leur consultation. Les tests automatisés conservent leur propre base et activent explicitement la démo.

# Comprendre et tester l'API avec Postman

## 1. Ce que fait l'application

Pour le moment, nous construisons le moteur du futur site étudiant.
Il sait lire des exemples de pages HTML, en extraire une entreprise et préparer un dossier structuré.
Il ne collecte pas encore d'offres réelles et ne dispose pas encore d'une interface graphique.

Une API est une porte d'entrée pour demander quelque chose à un programme.
Postman permet d'envoyer ces demandes et d'afficher les réponses.

```text
Vous cliquez sur Send dans Postman
    -> votre application Java reçoit la demande
    -> elle exécute le traitement demandé
    -> Postman affiche la réponse JSON
```

JSON est simplement un texte structuré avec des champs et des valeurs.
Par exemple, `"name": "Island Digital Studio"` signifie que le champ name contient ce nom.

- **GET** demande à lire des informations.
- **POST** demande ici de lancer l'import des fichiers de démonstration.
- **200 OK** signifie que la demande a réussi.
- **400 Bad Request** signifie qu'un paramètre envoyé est invalide.
- **404 Not Found** signifie que le dossier demandé n'existe pas.

## 2. Démarrer Java avant Postman

Dans IntelliJ :

1. Ouvrir le projet et recharger Maven avec **Reload All Maven Projects**.
2. Vérifier que le projet et Maven utilisent Java 21, puis lancer scripts/Start-Database.ps1 dans le terminal.
3. Exécuter le goal Maven **clean package**. Attendre **BUILD SUCCESS**, 109 tests réussis.
4. Ouvrir le terminal PowerShell intégré à IntelliJ, à la racine du projet.
5. Exécuter `.\scripts\Start-Api.ps1`.
6. Attendre les messages `Tomcat started on port 8080` et `Started StudentCareerApplication`.
7. Laisser ce terminal ouvert. Si vous l'arrêtez avec Ctrl+C, Postman ne peut plus contacter l'application.

Le terminal peut rester silencieux ensuite : c'est normal, le programme attend les demandes.
Postman ne démarre pas le serveur à votre place.

Si PowerShell bloque l'exécution du script, utiliser cette commande à la place, sans changer les réglages Windows :

```powershell
java -Djdk.net.unixdomain.tmpdir=target -jar target/student-career-platform-1.0-SNAPSHOT.jar
```

## 3. Préparer Postman

Utiliser l'application Postman de bureau pour contacter facilement le serveur sur votre PC.
Vous pouvez saisir les requêtes manuellement ci-dessous, ou importer la collection préparée :

1. Dans votre espace de travail Postman, ouvrir **Import** (selon la version : **Use resources or import > Import**).
2. Choisir le fichier `postman/Student-Career-Platform.postman_collection.json` dans le projet.
3. Ouvrir la collection **Student Career Platform - Premiers tests**.
4. Les requêtes sont numérotées de 01 à 11 : les exécuter dans cet ordre avec **Send**.

La collection contient `baseUrl = http://127.0.0.1:8080`. Aucun compte de l'application, clé API
ou réglage d'authentification n'est nécessaire. Ne pas ajouter de corps aux requêtes POST de démonstration.
La requête 04 mémorise automatiquement un identifiant pour la requête 05.
Les onglets de résultats de tests de Postman affichent les vérifications intégrées.

Aide officielle pour importer : https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data

## 4. Première demande : le serveur fonctionne-t-il ?

Choisir la méthode **GET**, saisir l'adresse, puis cliquer **Send** :

```text
http://127.0.0.1:8080/api/status
```

Réponse attendue : **200 OK**, avec notamment :

```json
{
  "status": "UP",
  "storage": "POSTGRESQL",
  "dataMode": "EXPLICIT_IMPORTS"
}
```

UP signifie que votre programme répond. POSTGRESQL indique que les dossiers sont désormais enregistrés sur disque. Cette réponse ne signifie pas qu'un site d'emploi a été consulté.

## 5. Regarder le catalogue vide

Envoyer **GET** :

```text
http://127.0.0.1:8080/api/companies
```

Au premier démarrage, la réponse contient `"items": []` et `"totalElements": 0`.
Ce n'est pas une erreur : nous n'avons encore rien importé.
Si vous avez déjà effectué l'import, le catalogue contient les dossiers précédents.

## 6. Charger les exemples fictifs

Choisir **POST**, mettre **Body > none**, puis envoyer :

```text
http://127.0.0.1:8080/api/imports/demo
```

Cette action fait réellement travailler le moteur Java : il lit quatre fichiers HTML intégrés,
extrait les informations, normalise les espaces, compare les doublons possibles et conserve les dossiers.
Vous n'avez aucun fichier à sélectionner et aucun texte à coller dans Body.

La réponse est **200 OK** avec notamment :

```json
{
  "mode": "FICTIONAL_DEMO",
  "total": 4,
  "summary": {
    "SUCCESS": 2,
    "POSSIBLE_DUPLICATE": 1,
    "NO_COMPANY": 1,
    "READ_ERROR": 0,
    "INVALID_SOURCE": 0
  }
}
```

Il s'agit d'un extrait : la réponse complète contient aussi les résultats détaillés.
Deux entreprises ont été trouvées normalement. Une autre page ressemble à la première : elle est
signalée comme doublon possible et conservée. Le dernier fichier ne contient pas de nom exploitable.
Un nom absent est un résultat de traitement, pas une panne de l'API.

## 7. Consulter ce qui a été importé

Envoyer à nouveau **GET** :

```text
http://127.0.0.1:8080/api/companies
```

Vous devez obtenir `"totalElements": 3` et trois éléments dans `items`.
Vous retrouverez Island Digital Studio, sa seconde source possible et Lagoon Learning Lab.

Chaque dossier contient :

- `id` : son identifiant ; il sert à demander ce dossier précis.
- `company` : nom, site, secteur et localisation extraits.
- `source` : document lu, catégorie et date de lecture.
- `importStatus` : réussite ou doublon possible.
- `duplicateOf` : source de la première correspondance, lorsqu'il y en a une.

`source.kind = FIXTURE` signifie données fictives de démonstration.
`source.url = classpath:/demo/...` désigne le fichier intégré réellement lu, pas une page web consultée.
Trois dossiers ne signifient donc pas trois entreprises différentes, ni trois entreprises qui recrutent.

## 8. Ouvrir un dossier précis

Dans la réponse précédente, copier la valeur d'un `id`, sans les guillemets.
Remplacer IDENTIFIANT ci-dessous par cette valeur et envoyer **GET** :

```text
http://127.0.0.1:8080/api/companies/IDENTIFIANT
```

Vous obtenez uniquement le dossier demandé. Avec la collection importée, exécuter simplement
la requête 04 puis la requête 05 : la copie de l'identifiant est automatique.

## 9. Rechercher et filtrer

Envoyer **GET** :

```text
http://127.0.0.1:8080/api/companies?q=island&location=ebene
```

`q=island` cherche ce texte dans le nom. `location=ebene` filtre la localisation.
Les deux conditions s'appliquent ensemble, sans tenir compte des majuscules/minuscules.
Vous devez obtenir **2 dossiers**, car les deux sources d'Island Digital Studio sont conservées.

Autre exemple :

```text
http://127.0.0.1:8080/api/companies?industry=education
```

Résultat attendu : **1 dossier**, Lagoon Learning Lab.
Dans Postman, vous pouvez également saisir ces paramètres dans l'onglet **Params**.

## 10. Voir une page de résultats

Envoyer **GET** :

```text
http://127.0.0.1:8080/api/companies?page=1&size=2
```

`size=2` demande au plus deux dossiers par page. Le comptage commence à 0 :
page 0 est la première page, page 1 est la deuxième. Il reste donc **un seul dossier** sur cette page.
Le total général reste 3 et le nombre de pages est 2.

## 11. Comprendre une erreur attendue

Envoyer **GET** :

```text
http://127.0.0.1:8080/api/companies?size=101
```

La réponse doit être **400 Bad Request** : la taille maximale autorisée est 100.
Le JSON explique l'erreur. Le serveur reste en fonctionnement.

Envoyer ensuite **GET** :

```text
http://127.0.0.1:8080/api/companies/00000000-0000-0000-0000-000000000000
```

La réponse doit être **404 Not Found** : cet identifiant est valide dans sa forme mais n'existe pas.
Un texte qui n'est pas un UUID, par exemple `abc`, donne 400.

## 12. Réimporter, puis arrêter

Renvoyer le POST d'import puis le GET du catalogue : il doit toujours y avoir **3 dossiers**.
Les mêmes sources sont actualisées, elles ne sont pas ajoutées une seconde fois.

Arrêter l’application avec Ctrl+C, puis la relancer. Le catalogue conserve désormais ses dossiers grâce à PostgreSQL.
Voir PERSISTENCE_GUIDE.md pour vérifier cette conservation sans refaire d’import.

Si Postman affiche `Could not send request` ou `ECONNREFUSED`, vérifier que Java fonctionne encore,
que le port est le bon et que l'adresse commence par `http://`, pas `https://`.
Une réponse JSON 400/404 est différente : le serveur a bien reçu votre demande et y a répondu.

## Validation à me communiquer

Dites-moi simplement : le GET status fonctionne-t-il, le POST affiche-t-il total 4,
et le GET companies affiche-t-il totalElements 3 ? En cas de difficulté, indiquez le numéro de l'étape
et le message affiché. Aucun commit ni push ne sera fait sans votre accord explicite.


## Extension : données réelles

Le parcours ci-dessus décrit les exemples fictifs. Un collecteur réel limité existe maintenant :
voir REAL_COLLECTION_GUIDE.md. Si vous l'avez déjà lancé dans la même session, totalElements peut valoir 6
au lieu de 3. Les trois fixtures restent présentes, identifiables par source.kind FIXTURE ; les trois autres
sont WEB_OBSERVATION et UNVERIFIED. L'exercice initial de pagination est décrit pour un catalogue de trois dossiers.


Mise à jour semaine 4 : démarrer la base dédiée avec scripts/Start-Database.ps1 avant le Run IntelliJ
ou les tests. Les anciennes indications de catalogue initialement vide ne s'appliquent qu'à une base
neuve : les dossiers persistés sont relus aux démarrages suivants. Voir PERSISTENCE_GUIDE.md.
