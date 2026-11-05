# Tester le premier collecteur réel dans Postman

## Ce qui change

Le POST de démonstration continue de lire les fichiers fictifs du projet.
Le nouveau POST Wikidata contacte réellement trois exports JSON connus sur Internet.
Il ne cherche pas toutes les entreprises et ne collecte aucune offre de stage.

Le catalogue est désormais enregistré dans PostgreSQL. L'API ne contacte jamais Wikidata au démarrage : vous déclenchez l'action.

## Préparer le nouveau code

1. Arrêter l'ancienne application dans IntelliJ, avec Stop.
2. Recharger Maven si nécessaire puis lancer clean package : **109 tests réussis**, BUILD SUCCESS.
3. Relancer StudentCareerApplication, avec les mêmes VM options qui fonctionnaient auparavant.
   Alternative : `.\scripts\Start-Api.ps1` dans le terminal PowerShell du projet.
4. Vérifier GET http://localhost:8080/api/status : UP.

## 1 — Voir ce qui sera lu, sans contacter la source

Créer une requête GET :

```text
http://localhost:8080/api/sources
```

Réponse attendue : 200, discoveryMode CURATED_SEEDS, trois URL et licence CC0-1.0.
CURATED_SEEDS signifie que la liste de départ a été préparée, pas découverte automatiquement.

## 2 — Déclencher la collecte réelle

Créer une requête POST, avec Body none :

```text
http://localhost:8080/api/imports/wikidata
```

L'application lit les trois fiches successivement. Cela peut prendre plusieurs secondes, au maximum
le temps des trois lectures limitées chacune à 15 secondes, plus les pauses.
La réponse contient mode WIKIDATA_OPEN_DATA et total 3.

**Lire summary avant de conclure à une réussite.** Au contrôle du 5 novembre, SUCCESS valait 3 et les
compteurs d'erreurs valaient 0. Une panne, un changement de contenu ou un refus peut modifier ce résultat.
200 signifie que le lot a été traité, pas que chaque lecture a réussi.
Chaque résultat dans results donne son statut et, s'il réussit, son dossier avec sa source.

## 3 — Consulter une entreprise réellement lue

Envoyer GET :

```text
http://localhost:8080/api/companies?q=emtel
```

Après une collecte réussie, un dossier Emtel doit apparaître. Vérifier :

- source.kind : WEB_OBSERVATION ;
- source.verificationStatus : UNVERIFIED ;
- source.url : export Wikidata réellement lu, pas le site de l'entreprise ;
- source.accessedAt : date de lecture de cette nouvelle exécution ;
- source.sourceUpdatedAt : date de modification annoncée par Wikidata, si présente ;
- source.license : CC0-1.0 ;
- company.industry : null, car le secteur n'est pas encore résolu ;
- company.location : Mauritius, issu de la déclaration du pays, pas d'une adresse vérifiée.

Le site de l'entreprise peut être affiché sans avoir été visité. Ces données ne prouvent aucune offre d'emploi.

## 4 — Comprendre la protection contre les répétitions

Un deuxième POST immédiatement après le premier donne **429 Too Many Requests** et Retry-After.
C'est attendu : patienter au moins une minute au lieu de cliquer plusieurs fois.
Une réponse 429/503 de Wikidata peut imposer une pause plus longue. Un accès refusé suspend la lecture.
La collection Postman inclut ce test : l'exécuter immédiatement après la collecte, sinon une minute peut
s'être écoulée et la nouvelle collecte serait à nouveau permise.

## 5 — Conserver les exemples fictifs séparés

Le POST /api/imports/demo est désormais désactivé par défaut. Dans un environnement de démonstration dédié où il est explicitement réactivé, Avec les deux imports réussis, le catalogue contient
six dossiers : trois observations Web et trois dossiers fictifs. Les catégories restent différentes.
Les requêtes de démonstration et le script Test-Api.ps1 vérifient les trois fixtures sans confondre le total général.
Après redémarrage, ces dossiers restent disponibles grâce à PostgreSQL.

La collection réelle est dans postman/Real-Collection.postman_collection.json. Importer ce fichier dans
Postman si souhaité, ou créer les requêtes manuellement comme précédemment.
Ne pas lancer cette collection en boucle ; elle effectue une vraie collecte externe.
Les tests automatisés Maven, eux, ne contactent jamais Wikidata.

Pour l'approbation du checkpoint : indiquer les compteurs du POST et si le dossier Emtel apparaît avec
sa source et le statut UNVERIFIED. Aucun commit ou push ne sera fait sans accord explicite.


Mise à jour semaine 4 : démarrer la base dédiée avec scripts/Start-Database.ps1 avant le Run IntelliJ
ou les tests. Les anciennes indications de catalogue initialement vide ne s'appliquent qu'à une base
neuve : les dossiers persistés sont relus aux démarrages suivants. Voir PERSISTENCE_GUIDE.md.
