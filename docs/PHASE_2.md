# Phase 2 — Opportunités et logique métier, semaines 5 à 7

## Ce qui est livré

### Semaine 5 : représenter les opportunités

Une opportunité est distincte d'une entreprise. Elle possède son identifiant, une entreprise associée,
un titre, un type INTERNSHIP ou JUNIOR_JOB, une description, un secteur et une localisation facultatifs,
un mode ON_SITE/HYBRID/REMOTE/UNKNOWN, une date de publication et une échéance facultatives.
Elle conserve sa source déclarée, la méthode de soumission, ses dates et les critères d'éligibilité connus.

La relation utilise l'UUID du dossier d'entreprise existant, avec une clé étrangère PostgreSQL.
Le projet ne prétend toujours pas fusionner automatiquement plusieurs dossiers d'une même entreprise.
La migration Flyway V2 ajoute opportunities et opportunity_reviews ; V1 reste inchangée.
Les opportunités et leurs décisions sont persistées dans PostgreSQL.

### Semaine 6 : détection, vérification et actualisation

- Saisie structurée manuelle, toujours UNVERIFIED au départ.
- Détection de JobPosting dans des scripts JSON-LD de HTML fourni à l'application.
- Reconnaissance des stages explicitement désignés INTERN/INTERNSHIP et des titres contenant Junior.
  Un poste FULL_TIME, INTERNATIONAL ou un nom d'entreprise n'est pas transformé en offre junior.
- L'employeur du JobPosting doit correspondre au nom du dossier sélectionné ; sinon la candidature à l'import est rejetée.
- Les graphes et tableaux JSON-LD sont acceptés ; aucun contexte distant n'est chargé.
- Confirmation ou rejet manuel avec consultation explicitement déclarée, preuve et notes.
- Fermeture, réouverture, historique des décisions et contrôle des versions concurrentes.
- Expiration calculée au moment de la lecture ; aucune tâche programmée n'est nécessaire.
- Une confirmation datant de 30 jours ou plus devient STALE tant qu'elle n'est pas renouvelée.
- Un changement matériel invalide la confirmation. Un HTML identique réimporté ne renouvelle pas cette confirmation.
- Le réimport structuré retrouve le même identifiant grâce à sa clé de dédoublonnage.

### Semaine 7 : recherche et pertinence

Recherche par mots du nom du poste, de sa description et de l'entreprise, avec accents/casse/espaces normalisés.
Filtres secteur, localisation, type, mode de travail, état, confirmation fraîche et public étudiant.
Pagination bornée et tris RELEVANCE, NEWEST, CLOSING_SOON.
Les recherches d'entreprises existantes bénéficient aussi de la normalisation des accents.

Les deux publics sont traités explicitement : MAURITIAN et INTERNATIONAL. Les valeurs YES, NO, UNKNOWN
représentent des indications déclarées dans la source. Aucune règle juridique sur les permis, visas ou
la nationalité n'est déduite automatiquement. Les détails de la source sont obligatoires lorsqu'une valeur
YES ou NO est saisie. Une localisation à Maurice ne signifie jamais qu'un public est éligible.

## Ce que cette phase ne prétend pas faire

Il n'y a toujours pas de recherche automatique générale d'offres sur Internet.
Le collecteur Wikidata découvre des entreprises sur une liste préparée, pas des postes.
L'import d'offres prend un HTML soumis : sourceFetchedByServer est false et sourceAccessedAt est null.
sourceUrl désigne la provenance déclarée par l'opérateur ; le serveur ne prétend pas l'avoir visitée.
Les dates firstSubmittedAt/lastSubmittedAt enregistrent la réception, pas une consultation du site tiers.
Une nouvelle collecte de sites employeurs exigera le choix et la validation d'une source autorisée.

CONFIRMED signifie une décision manuelle enregistrée par l'opérateur local, avec sa déclaration de consultation.
Ce n'est ni une certification externe ni une preuve cryptographique. Les comptes et l'identité du vérificateur
arrivent à la phase 3 ; l'API reste locale et n'est pas destinée à être publiée sans authentification.
Aucune annonce réelle n'est inventée, et aucune annonce synthétique de validation n'est ajoutée à la base applicative.

## États effectifs

| État | Sens |
| --- | --- |
| UNVERIFIED | Information soumise qui nécessite une vérification |
| OPEN | Confirmation manuelle fraîche et échéance non dépassée |
| STALE | Confirmation ancienne de 30 jours ou plus, à renouveler |
| CLOSED | Fermeture explicite |
| EXPIRED | Échéance atteinte ou dépassée |
| REJECTED | Rejet explicite après examen déclaré |

La priorité de calcul est REJECTED, CLOSED, EXPIRED, puis fraîcheur de la vérification.
Sans échéance connue, aucune date de fin n'est inventée : une confirmation peut néanmoins devenir STALE.
Une échéance datetime conserve son décalage et son instant précis. Une date seule est interprétée en fin
de journée dans Indian/Mauritius, avec précision à la microseconde de PostgreSQL.
Une date de publication future ou une échéance antérieure à la publication est refusée.

Le GET liste les états UNVERIFIED, OPEN et STALE par défaut. CLOSED/EXPIRED/REJECTED sont exclus sauf
includeInactive=true ou un filtre status explicite. verifiedOnly=true exige l'état OPEN, pas simplement
une ancienne confirmation. Le détail reste consultable quel que soit l'état.

## Préserver les décisions

Chaque opportunité retourne version. PUT et POST review doivent fournir la version courante.
Une version absente est invalide (400) ; une version périmée provoque 409 sans écraser le changement récent.
La colonne JPA @Version couvre aussi les écritures concurrentes.
Les décisions sont conservées dans opportunity_reviews. Il n'existe pas de DELETE API qui effacerait ces traces.
L'historique porte sur les décisions et changements, pas encore sur une copie intégrale de chaque version du texte.

Une annonce fermée reste fermée lors d'un réimport. Une modification ne la rouvre pas automatiquement.
REOPEN la remet en UNVERIFIED ; CONFIRM est une action distincte. Une annonce expirée ne peut pas être
confirmée ou rouverte tant que son échéance n'a pas été corrigée avec une justification sur la source.

## Doublons

La clé est calculée sur dossier d'entreprise + URL canonique de source + référence externe si présente,
sinon titre normalisé. HTTP(S), hôte et ports par défaut sont normalisés ; fragments retirés, chemin et
paramètres conservés. Les identifiants externes restent sensibles à la casse.
POST refuse le doublon en 409 ; l'import HTML actualise le même dossier quand sa clé est retrouvée.
Sans référence externe, un titre qui change peut nécessiter un rapprochement manuel.
Des dossiers d'entreprise différents ne sont pas fusionnés sous prétexte de noms similaires.

## Pertinence explicable

Le score n'est pas une mesure de véracité et n'utilise pas d'IA :
- mot présent dans le titre : +3 ; sinon dans la description : +1 ;
- filtre secteur ou localisation correspondant : +2 chacun ;
- éligibilité YES déclarée pour le public sélectionné : +2.

matchedCriteria expose les motifs. Les critères servent d'abord de filtres ; le score ordonne les résultats.
Les valeurs UNKNOWN ne sont pas assimilées à YES : studentType exclut UNKNOWN par défaut, tandis que
includeUnknownEligibility=true permet de les afficher en les conservant marquées UNKNOWN.
Sans studentType, aucun public n'est favorisé. Le modèle de profil étudiant personnalisé viendra ensuite.
Les filtres sont actuellement appliqués en Java après lecture PostgreSQL. Ce choix est adapté au petit jeu
initial ; une recherche SQL dédiée sera nécessaire avant un grand volume de données.

## Référence de détection

Le format de document ciblé est Schema.org JobPosting : https://schema.org/JobPosting
Les champs applicantLocationRequirements et jobLocation ne sont pas interprétés comme des règles de citoyenneté
ou de visa. Le texte eligibilityToWorkRequirement est conservé comme indication, sans en déduire YES/NO.
Au plus 25 annonces, 30 scripts JSON-LD et 262144 caractères de HTML par soumission.
Les erreurs d'un candidat sont rapportées sans inventer un résultat ; les erreurs de base annulent la transaction.

## Routes

| Méthode | Adresse | Fonction |
| --- | --- | --- |
| POST | /api/opportunities | Soumettre une opportunité |
| GET | /api/opportunities | Rechercher et filtrer |
| GET | /api/opportunities/{id} | Lire un dossier |
| PUT | /api/opportunities/{id} | Modifier avec version |
| POST | /api/opportunities/{id}/review | CONFIRM, REJECT, CLOSE ou REOPEN |
| GET | /api/opportunities/{id}/reviews | Historique |
| POST | /api/opportunities/imports/html | Détecter et importer le HTML soumis |

POST retourne 201 avec Location. Les erreurs sont 400 (données invalides), 404 (ressource absente),
409 (doublon/version/action incompatible). Les routes des entreprises restent disponibles.
Voir PHASE_2_POSTMAN.md pour les corps JSON et le parcours de validation isolé.
