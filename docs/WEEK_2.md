# Semaine 2 — collecte locale structurée

## Objectifs livrés

- Produire contenu, URI effective et date dans le lecteur, plutôt que les assembler séparément.
- Déclarer explicitement la catégorie FIXTURE ou LOCAL_DOCUMENT.
- Normaliser les textes sans inventer les informations manquantes.
- Traiter plusieurs sources dans l'ordre et isoler les erreurs par entrée.
- Signaler les doublons possibles sans supprimer ni fusionner de dossiers.
- Exporter le bilan et les dossiers avec leur provenance.
- Fournir des fixtures, deux démonstrations et une procédure IntelliJ dans le README.

## Choix Java à comprendre

Un `record`, ici FetchedSource ou SourceRequest, regroupe des valeurs liées sans écrire
manuellement les accesseurs. Le HTML et sa provenance circulent ensemble.
Les objets Company et SourceReference sont eux aussi non modifiables par leurs accesseurs.

Une `enum` définit un ensemble fermé de valeurs : SourceKind et BatchStatus évitent les
fautes de frappe et rendent les catégories et résultats explicites.

Une `List` conserve l'ordre des entrées. Une `Map` associe une clé de comparaison à la
première source rencontrée. La clé est un record, afin de comparer ses champs ensemble
sans construire une chaîne ambiguë avec des séparateurs.

Une `Clock` est fournie au lecteur pour contrôler l'heure pendant les tests. En exécution
normale, Clock.systemUTC() utilise l'heure réelle. Aucune date de test n'est utilisée dans les démos.

Le service transforme chaque lecture en dossier. Le pipeline individuel et le service de lot
réutilisent ce service : ils ne reproduisent pas le parsing ou la construction de provenance.

## Changements de contrat

- SourceFetcher : fetchSource(URI, SourceKind) retourne FetchedSource ; fetch(URI) reste
  un raccourci pour obtenir du texte depuis un document local.
- CompanyDossierPipeline : execute(URI, SourceKind) remplace execute(URI, SourceReference).
- CompanyIntelligenceService : analyze(URI) reste disponible ; analyzeDossier ajoute la provenance.
- JSON : source.kind est ajouté. Les autres clés du dossier individuel sont conservées.
- Les démonstrations et les tests concernés ont été adaptés ensemble.

## Limites assumées

- Une source locale peut être réelle ou fictive : sa catégorie reste déclarée par l'appelant.
- Une lecture locale ne prouve jamais une consultation distante ou une information vérifiée.
- Le nom et le site ne suffisent pas à garantir une identité : le statut est POSSIBLE_DUPLICATE.
- Les chemins et paramètres des sites peuvent représenter des entreprises différentes : ils sont conservés.
- Aucun rapprochement approximatif, fusion métier, stockage persistant ou suivi temporel.
- Les erreurs contiennent des chemins locaux : les exports de démonstration restent locaux et ignorés par Git.

## Conditions avant une collecte web

1. Choisir une source identifiée et explicitement autorisée, idéalement une API ou un jeu de données officiel.
2. Vérifier ses conditions d'utilisation et les règles d'accès avant de coder le lecteur.
3. Définir une fréquence raisonnable, les délais, limites de taille et erreurs réseau attendues.
4. Enregistrer l'URI effectivement consultée après redirection et la date ; conserver la catégorie
   et introduire un état de vérification distinct de la simple lecture.
5. Définir les champs autorisés et conserver les informations inconnues sans les inventer.
6. Tester d'abord avec des réponses locales ou un serveur de test, sans dépendre d'un site tiers.

Aucune source réelle n'a été évaluée ou contactée durant cette semaine. Ces points sont un plan,
pas une affirmation qu'une collecte web est déjà autorisée.

## Vérification

Compilation complète et suite Maven/JUnit : 62 tests réussis, aucun échec, erreur ou test ignoré.
Démonstrations exécutées : dossier individuel, lot de six entrées et lecture du répertoire de fixtures.
La validation dans IntelliJ a été confirmée par l’utilisateur, qui a autorisé le commit et le push.
