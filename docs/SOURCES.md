# Sources réelles — décision du 5 novembre 2026

## Sources repérées et choix initial

| Source | Intérêt | Décision initiale |
| --- | --- | --- |
| Mauritius Open Data / MDPA — List of ICT Companies in Mauritius | Jeu CSV d'entreprises TIC, adresse et activités | Non retenu pour l'automatisation actuelle : robots.txt annonce Disallow / pour tous les agents. Licence déclarée CC-BY-SA-4.0 ; ne pas contourner la règle d'accès. |
| MDPA — ICT Directory | Annuaire institutionnel d'entreprises TIC | Source de découverte repérée ; pas d'adaptateur construit et conditions d'exploitation à établir. |
| MCCI — Members Directory | Entreprises, secteurs et coordonnées | Source repérée ; réutilisation automatisée non établie durant cet audit, donc pas de collecte. |
| CareerHub | Entreprises et offres | Hors du premier collecteur d'entreprises ; aucune extraction des offres ni autorisation supposée. |
| Wikidata — exports JSON de fiches connues | Données structurées, licence CC0, méthode d'accès documentée | Retenu pour trois fiches connues, à partir d'URL explicitement inscrites dans le code. |

Le portail Open Data a été évalué avec son API de métadonnées et son robots.txt. Aucune réutilisation
récurrente n'est implémentée. Le caractère public ou une licence de données n'est pas considéré comme
une permission de contourner les règles techniques de collecte.

## Références consultées

- Open Data Mauritius : https://data.govmu.org/en/
- Métadonnées TIC : https://data.govmu.org/api/3/action/package_show?id=f65a8b55-500e-4f79-9999-fed2118cc4e4
- Règles d'accès Open Data : https://data.govmu.org/robots.txt
- MDPA : https://mdpa.govmu.org/mdpa/index.php/ict-directory/
- MCCI : https://www.mcci.org/en/membership/members-directory/
- Licence Wikidata : https://www.wikidata.org/wiki/Wikidata:Licensing
- Accès Wikidata : https://www.wikidata.org/wiki/Wikidata:Data_access#Linked_Data_Interface_(URI)
- Règles Wikidata : https://www.wikidata.org/robots.txt

Les données structurées de Wikidata sont publiées sous CC0. La documentation recommande l'interface
Linked Data pour un petit nombre d'entités déjà connues. Le robots.txt distingue les pages spéciales
génériques des exports EntityData avec extension ; ces exports JSON sont explicitement autorisés.
Le prototype n'utilise pas le service SPARQL : une requête exploratoire a expiré et cette méthode n'est
pas nécessaire pour notre liste initiale. Il ne lance aucune indexation des pages de recherche.

## Liste préparée, pas recherche exhaustive

Les trois références ont été identifiées pendant la recherche de sources et vérifiées par leur fiche :

- Q3301671 : https://www.wikidata.org/wiki/Special:EntityData/Q3301671.json
- Q3053417 : https://www.wikidata.org/wiki/Special:EntityData/Q3053417.json
- Q3301668 : https://www.wikidata.org/wiki/Special:EntityData/Q3301668.json

L'application n'invente pas de nouveaux QID, ne parcourt pas les liens et ne visite pas les sites officiels
mentionnés dans les fiches. Changer cette liste exige une décision sur la source et une modification du code.
L'étape de découverte reste donc initialement humaine et guidée ; aucune recherche autonome générale
ou découverte de toutes les entreprises mauriciennes n'est annoncée.

## Règles de lecture

- User-Agent honnête : StudentCareerPlatform/1.0 avec le dépôt GitHub comme contact public.
- GET HTTPS uniquement vers les trois URL de production autorisées ; aucun URL fourni par HTTP n'est accepté.
- Aucun suivi des redirections, aucune authentification ou tentative de contournement.
- Une seconde au minimum entre les lectures ; aucune requête parallèle ni tentative automatique répétée.
- Une tentative d'import par minute, y compris après un échec.
- Connexion limitée à 5 secondes ; délai global d'une lecture limité à 15 secondes.
- Au plus 512 Kio pour la réponse et pour le contenu après décompression gzip.
- Réponses 429/503 : pause du lecteur, au moins une minute et respect d'un Retry-After plus long.
- Réponses 401/403 : suspension du lecteur pendant une heure, sans nouvelle tentative sur les autres fiches.
- Aucun appel externe au démarrage ni pendant la suite de tests Maven.

Les règles de la source peuvent changer : cette décision doit être revue avant d'élargir ou de planifier la collecte.

## Données extraites et sens des dates

Le lecteur produit contenu et SourceReference ensemble, avec l'URL effectivement demandée et reçue.
Chaque réponse doit contenir exactement l'identifiant d'entité attendu, pour ne pas attribuer la mauvaise fiche.

Le parseur extrait :

- Nom : libellé anglais, sinon français. Un QID n'est jamais présenté comme un nom.
- Site : déclaration P856 non dépréciée et sans fin déclarée ; URL HTTP(S) avec hôte et sans identifiants.
- Pays : P17 doit déclarer Mauritius (Q1027), avec priorité aux déclarations préférées.
- Secteur et ville : inconnus dans cette version ; P452 et P159 contiennent des références qui nécessitent
  une résolution supplémentaire. Aucune valeur n'est devinée à partir du nom ou de la description.
- Une fiche comportant une déclaration actuelle de dissolution P576 est écartée.

Les déclarations dépréciées ou portant une date de fin P582 ne sont pas utilisées. Le traitement des dates
est volontairement conservateur : une date de fin/dissolution future peut aussi faire écarter une déclaration.
Plusieurs sites courants : priorité au rang préféré, puis premier site valide dans l'ordre lexical.

accessedAt est la fin de lecture du fichier JSON. sourceUpdatedAt est le champ modified annoncé par Wikidata,
ou null s'il manque. Il ne s'agit ni d'une date de vérification indépendante ni d'une garantie de fraîcheur.
Les données sont toujours UNVERIFIED. La licence CC0 décrit la réutilisation, pas la vérité des informations.
Aucun numéro de téléphone, nom de contact ou e-mail personnel n'est extrait ou enregistré.
Le JSON complet est traité en mémoire puis abandonné ; seuls les champs du dossier et la provenance sont conservés.

## Fiabilité et limites restantes

La fiabilité livrée concerne la traçabilité, les contrôles d'entrée et les erreurs, pas une certification métier.
Les informations doivent encore être recoupées avec une source autorisée de l'entreprise avant d'être qualifiées
comme vérifiées. Une entreprise existante n'est pas une entreprise qui recrute.

Un lot partiellement ou totalement échoué retourne ses compteurs et ses erreurs, sans dossier inventé.
Si une source déjà importée échoue ou devient inéligible, l'ancien dossier n'est pas effacé automatiquement :
il garde sa date précédente et ne doit pas être présenté comme actualisé. Il n'existe pas encore de statut périmé.

L'identité du dossier repose sur la catégorie et l'URL : une source correspond à un dossier. Les différentes
sources d'une entreprise ne sont pas fusionnées. Le modèle entreprise/sources et la gestion de leur historique
seront précisés pour la persistance ; aucune base n'est encore ajoutée.

## Vérification obtenue

Le 5 novembre 2026 : le collecteur Java a lu les trois exports, créé trois dossiers avec statut UNVERIFIED,
et le réimport immédiat a été refusé en HTTP 429. Les entreprises sont Mauritius Telecom, Emtel et Mauritius
Commercial Bank. Aucun de leurs sites officiels n'a été consulté par le collecteur.

100 tests Maven/JUnit réussis, dont limites HTTP, gzip, délais, identité de réponse, refus d'accès, erreurs,
normalisation, doublons et parcours API existants. Les tests utilisent des données synthétiques ; la lecture
réelle a été vérifiée séparément. Le bilan filtré de cette exécution est généré sous target/real-collection-report.json,
ignoré par Git. Aucun jeu de données réel complet n'est ajouté au dépôt.
