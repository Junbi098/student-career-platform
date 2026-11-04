# Student Career Intelligence Platform

POC Java 21 pour organiser les informations sur les entreprises utiles aux étudiants à Maurice.
Les entreprises des fixtures sont fictives. Aucun résultat ne signifie qu'une entreprise recrute.
Aucun site web réel n'est consulté par cette application.

## Outils

Java 21, Maven, jsoup 1.23.2, JUnit Jupiter 5.12.2, Surefire 3.5.3 et Jackson 2.22.4.
Ouvrir le dossier comme projet Maven dans IntelliJ et utiliser le JDK 21.
Le Maven intégré à IntelliJ suffit ; aucune modification du PATH n'est nécessaire.

## Architecture

```text
URI locale + catégorie
  -> LocalFileSourceFetcher : HTML + provenance de lecture
  -> CompanyIntelligenceService : parsing + normalisation + CompanyDossier
  -> CompanyDossierPipeline : JSON individuel

Liste de SourceRequest
  -> CompanyBatchService : un résultat par entrée, dans le même ordre
  -> CompanyBatchJsonWriter : résumé + résultats + dossiers et sources conservés
```

- Le nom est obligatoire ; les autres champs peuvent être inconnus (`null`).
- Le premier élément de chaque sélecteur est utilisé. Un premier nom vide donne une absence d'entreprise.
- Une URL de site doit être HTTP(S), absolue, avec un hôte ; sinon le site reste inconnu.
- jsoup tolère du HTML incomplet ; cette tolérance ne vérifie pas la vérité des informations.
- Le service normalise les espaces Unicode et les accents en forme NFC, sans changer la casse affichée.
- Les chemins et paramètres des sites web sont conservés.
- `Optional.empty()` signifie qu'aucun nom d'entreprise n'a été trouvé.
- En mode individuel, les erreurs de lecture remontent comme `IOException`.

## Provenance

Le lecteur retourne `FetchedSource`, qui associe le HTML à sa `SourceReference`.
Il résout le chemin réel (`toRealPath`), lit le contenu UTF-8, puis date la fin de lecture.
Une `Clock` injectable rend les tests de date reproductibles, sans attendre le temps réel.
Le pipeline prend désormais `execute(URI, SourceKind)` : il n'accepte plus une référence
indépendante qui pourrait déclarer une autre URL ou une date arbitraire.
Le JSON conserve `company` et `source` ; `source.kind` est un nouveau champ.

Catégories :

- `FIXTURE` : données fictives déclarées comme telles par l'appelant.
- `LOCAL_DOCUMENT` : document local, sans preuve de consultation d'un site web ou de véracité.
- `UNCLASSIFIED` : ancienne référence construite manuellement ; refusée à l'entrée du lecteur local.

La catégorie est une déclaration de l'appelant, pas une détection automatique du contenu.
La date décrit la lecture du fichier local, pas sa date de publication ni la consultation d'un site distant.
Les constructions manuelles de `SourceReference` restent possibles pour les modèles et les tests,
mais le pipeline utilise exclusivement la référence produite par le lecteur.

## Traitement par lot et doublons

| Statut | Signification |
| --- | --- |
| `SUCCESS` | Entreprise extraite et dossier conservé |
| `POSSIBLE_DUPLICATE` | Correspondance avec une entrée précédente, dossier également conservé |
| `NO_COMPANY` | Aucun nom extrait |
| `READ_ERROR` | Fichier absent, répertoire ou autre erreur de lecture |
| `INVALID_SOURCE` | URI ou catégorie non prise en charge, sans consultation web |

Le lot continue après une erreur de lecture ou une entrée invalide.
Une liste nulle ou contenant une requête nulle est une erreur d'appel, refusée avant traitement.

La comparaison exige le même nom normalisé, comparé sans tenir compte de la casse, et le même
site web. Pour cette comparaison uniquement : schéma et hôte sont comparés en minuscules,
les ports HTTP/HTTPS par défaut sont ignorés et un chemin vide équivaut à `/`.
Les fragments ne différencient pas les sites ; chemin, paramètres et éventuels identifiants restent distincts.
Les catégories de source restent séparées : une fixture n'est jamais regroupée avec un document local.
Sans site web, aucun doublon n'est proposé à partir du seul nom.

Ces correspondances sont des pistes à vérifier, pas une résolution certaine de l'identité.
Aucune donnée n'est fusionnée ou supprimée. `duplicateOf` référence la première source correspondante.
Le JSON conserve chaque dossier et sa provenance, même en cas de doublon possible.
Le traitement est séquentiel, en mémoire ; il ne conserve pas d'historique entre deux lots.

## Checkpoint local dans IntelliJ

1. Dans Maven, exécuter `clean test` (ou `mvn clean test` si Maven est accessible au terminal).
2. Vérifier **62 tests**, zéro échec, zéro erreur, zéro test ignoré et **BUILD SUCCESS**.
3. Ouvrir `src/test/java/com/studentcareer/poc/demo/CompanyDossierDemo.java` et lancer `main()`.
4. Vérifier `target/demo-output/company-dossier.json` : entreprise Island Digital Studio,
   source `file:`, catégorie `FIXTURE` et date de cette nouvelle lecture.
5. Ouvrir `src/test/java/com/studentcareer/poc/demo/CompanyBatchDemo.java` et lancer `main()` sans argument.
6. Vérifier `target/demo-output/company-batch.json` : total 6, `SUCCESS` 2 et chacun des quatre autres statuts 1.
7. Dans `results`, le doublon conserve son dossier, sa propre source et le lien `duplicateOf`.
   Les entrées en erreur n'ont pas de dossier et ne bloquent pas les entrées suivantes.

Le répertoire de travail des deux démonstrations doit être la racine du projet.
La démonstration mixte inclut volontairement un fichier absent et une URL web non lue.
Les deux erreurs signalées dans son JSON sont donc attendues, sans échec du programme.
Chaque exécution remplace uniquement son fichier de sortie sous `target/demo-output/`.

### Essayer un répertoire de fixtures

Dans la configuration d'exécution de `CompanyBatchDemo`, saisir dans **Program arguments** :

```text
src/test/resources/fixtures/batch FIXTURE
```

Résultat attendu : 3 entrées, 2 réussites, 1 entreprise absente, aucune autre erreur.
Seuls les fichiers `.html` directement présents sont lus, dans l'ordre des chemins ; les sous-dossiers
ne sont pas parcourus. Pour un autre répertoire, utiliser `"C:/chemin avec espaces" FIXTURE`
ou `"C:/chemin avec espaces" LOCAL_DOCUMENT` selon la nature des documents.
Le mode sans argument reste disponible en vidant le champ Program arguments.
Un répertoire invalide arrête la démonstration ; un fichier individuel illisible n'arrête pas le lot.

## Avant les sources réelles

Voir [les décisions de la semaine 2](docs/WEEK_2.md).
Aucun fetcher HTTP, navigateur automatisé, base de données ou backend Spring n'est encore installé.
Les fichiers `target/` et `.idea/` restent exclus de Git.
