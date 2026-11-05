# Organisation actuelle du projet

Le code est organisé par sujet. Les noms des packages sont en anglais, comme habituellement en Java.

```text
com.studentcareer
├── StudentCareerApplication   démarrage de l'application
├── company                    dossiers et traitements des entreprises
│   ├── domain                 Company, CompanyDossier, CompanyCatalogEntry
│   ├── parsing                lecture des champs dans le HTML
│   ├── service                analyse, normalisation et recherche
│   ├── repository             contrat de stockage des dossiers
│   ├── persistence            entités JPA et adaptation PostgreSQL
│   ├── importing              traitement par lot et import de démonstration
│   ├── export                 génération des fichiers JSON
│   └── exception              dossier introuvable
├── source
│   ├── model                  provenance, catégorie, contenu lu
│   └── reader                 lecteurs de fichiers locaux et de fixtures intégrées
├── web
│   ├── controller             adresses que Postman peut appeler
│   ├── dto                    format des réponses JSON
│   └── error                  réponses en cas d'erreur
└── common.exception           requête invalide
```

Les tests suivent les mêmes packages sous src/test/java. Le double en mémoire y est réservé aux tests unitaires. Les exemples d'export manuels sont
séparés dans le package examples, sous src/test/java ; ils ne font pas partie du serveur.

## Suivre une demande Postman dans le code

Pour GET /api/companies :

1. CompanyController reçoit les paramètres.
2. CompanyCatalogService applique la recherche et la pagination.
3. CompanyRepository fournit les dossiers via JpaCompanyRepository et PostgreSQL.
4. CompanyResponses prépare les champs de la réponse JSON.

Pour POST /api/imports/demo :

1. DemoImportController reçoit la demande d'import.
2. DemoCompanyImportService prépare la liste des quatre fixtures.
3. CompanyBatchImportService traite chaque source avec CompanyAnalysisService.
4. BundledFixtureSourceReader lit le HTML et produit sa provenance.
5. CompanyHtmlParser extrait les champs ; CompanyNormalizer nettoie le texte.
6. Le lot signale les doublons possibles et le dépôt conserve les dossiers.

## Renommages principaux

| Ancien nom | Nouveau nom | Rôle |
| --- | --- | --- |
| CompanyIntelligenceService | CompanyAnalysisService | Analyse d'un document |
| CompanyBatchService | CompanyBatchImportService | Analyse d'un lot |
| InMemoryCompanyCatalog | InMemoryCompanyRepository | Stockage en mémoire |
| CatalogEntry | CompanyCatalogEntry | Dossier identifié dans le catalogue |
| DemoImportService | DemoCompanyImportService | Import des données fictives |
| SourceFetcher | SourceReader | Contrat de lecture |
| LocalFileSourceFetcher | LocalFileSourceReader | Lecture d'un fichier local |
| ClasspathFixtureSourceFetcher | BundledFixtureSourceReader | Lecture d'un exemple intégré |
| FetchedSource | SourceDocument | Contenu et provenance associés |
| BatchItem / BatchResult / BatchStatus | SourceImportResult / BatchImportResult / ImportStatus | Résultats d'import |
| CompanyDossierPipeline | CompanyDossierExportService | Export individuel en JSON |
| ApiResponses | CompanyResponses | Réponses HTTP |
| InvalidCatalogQueryException | InvalidRequestException | Paramètres invalides, y compris un corps d'import refusé |

Le projet Maven s'appelle désormais student-career-platform, comme le dépôt.
Le JAR est target/student-career-platform-1.0-SNAPSHOT.jar et le lanceur a été adapté.
Les routes HTTP et les champs JSON n'ont pas changé.

## Nettoyage effectué

- Suppression du Main initial qui affichait des nombres.
- Suppression du test d'addition FirstTest, sans rapport avec le produit.
- Suppression de CompanyNameExtractor, qui ne faisait que déléguer à CompanyHtmlParser.
- Suppression des quatre tests de ce wrapper : extraction, absence de nom, premier nom et null
  sont déjà vérifiés dans CompanyHtmlParserTest.
- Suppression des anciennes arborescences poc, api et catalog devenues vides après déplacement.
- Regroupement des fixtures HTML identiques dans src/main/resources/demo, utilisées aussi par les tests.
- Conservation des exemples d'export, des sources locales, des tests métier et de l'historique documentaire.
- Aucun nettoyage des réglages personnels IntelliJ, de Git ou de l'environnement Windows.

Les notes WEEK_2.md et WEEK_3.md sont des traces historiques ; ce fichier décrit les noms actuels.


## Complément de collecte réelle

CompanyAnalysisService accepte maintenant CompanyContentParser : l'implémentation HTML conserve
son comportement et WikidataCompanyParser traite les fiches JSON. SourceDocument.content() remplace
l'ancien nom html(), car un document peut contenir du HTML ou du JSON.

WikidataSources définit les trois références connues. WikidataSourceReader réalise les lectures limitées,
valide l'identité reçue et produit les dates de provenance. WikidataCompanyImportService réutilise le service
de lot existant, limite les déclenchements puis enregistre les dossiers dans le même dépôt en mémoire.
WikidataImportController expose GET /api/sources et POST /api/imports/wikidata, sans paramètre d'URL arbitraire.

Le JSON source ajoute verificationStatus, sourceUpdatedAt et license. Les données fictives restent FICTIONAL ;
les documents locaux et observations Web sont UNVERIFIED. Aucun statut VERIFIED n'est produit.


## Semaine 4 — persistance

Les services dépendent du contrat CompanyRepository. L'implémentation JpaCompanyRepository et les entités
CompanyDossierEntity / SourceReferenceEntity sont dans company.persistence. Le double en mémoire ne
reste que dans src/test/java. Flyway crée les tables avant leur validation par Hibernate.
L'adaptateur retourne les objets métier déjà utilisés par les contrôleurs : les routes restent inchangées.
Voir WEEK_4.md pour le modèle d'observation, l'isolation des tests et le schéma.
