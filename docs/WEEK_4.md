# Semaine 4 — PostgreSQL, JPA et Flyway

## Résultat concret

Les dossiers et leurs sources sont enregistrés sur disque dans PostgreSQL.
Arrêter Java puis le redémarrer ne les efface plus. Un réimport actualise les mêmes dossiers
sans changer leurs identifiants ni ajouter de lignes inutiles.

Le checkpoint a été exécuté avec trois dossiers fictifs : arrêt du processus Java, arrêt de l'instance
PostgreSQL du projet, redémarrage des deux, puis lecture des mêmes dossiers et provenances avant tout
nouvel import. Le POST de démonstration a ensuite conservé le même nombre et les mêmes identifiants.

## Les trois outils

- PostgreSQL est le programme qui stocke les données sur disque.
- Spring Data JPA relie les objets Java aux lignes des tables.
- Flyway applique des fichiers SQL numérotés pour créer et faire évoluer les tables.

Flyway crée la structure ; Hibernate la vérifie avec ddl-auto=validate. Il ne recrée ni ne modifie
silencieusement les tables. Le nettoyage Flyway est désactivé et aucun baseline automatique n'est demandé.
La migration initiale est src/main/resources/db/migration/V1__create_company_dossiers_and_sources.sql.
Une migration déjà appliquée doit rester intacte ; une évolution demandera un nouveau fichier versionné.

Référence officielle : https://docs.spring.io/spring-boot/how-to/data-initialization.html

## Choix du modèle

Nous ne savons pas encore certifier que deux noms représentent la même entreprise.
Le stockage conserve donc les dossiers d'observation existants, sans fusion fondée sur une simple ressemblance.

- company_dossiers : identifiant, nom, site, secteur, localisation, statut d'import et lien de doublon possible.
- source_references : URL lue, catégorie, titre, licence, date de lecture, date annoncée par la source,
  statut de vérification et place réservée à la date de vérification indépendante.
- flyway_schema_history : migrations appliquées par Flyway.

Chaque dossier possède sa propre source, reliée par une clé étrangère et une contrainte unique.
L'identifiant est toujours calculé à partir de la catégorie et de l'URL, comme avant cette semaine.
L'ordre de première insertion est conservé pour stabiliser la pagination et ne change pas au réimport.
Le compteur technique d'ordre peut contenir des trous après une transaction annulée : c'est normal.

La table représente des dossiers, pas un registre d'entreprises uniques certifiées. Le rapprochement entre
plusieurs sources d'une même entreprise et un futur modèle d'identité métier demandera une décision explicite.
Les observations fictives et Web ne sont pas fusionnées.

Les états actuels sont FICTIONAL ou UNVERIFIED. verified_at / verifiedAt reste null.
Il ne faut pas remplacer une date de vérification inconnue par la date de lecture.
La date sourceUpdatedAt reste celle déclarée par le fournisseur. PostgreSQL conserve les instants à la
microseconde ; cela ne change pas leur sens et les tests utilisent cette précision.

## Organisation du code

CompanyRepository définit le contrat de stockage utilisé par les services.
JpaCompanyRepository utilise Spring Data JPA et transforme les entités persistées en objets métier.
Les modèles Company et SourceReference restent utilisables sans JPA.
InMemoryCompanyRepository est désormais uniquement un double dans src/test/java pour les tests unitaires.
Les tests HTTP et de persistance utilisent de vraies connexions PostgreSQL, pas une base H2 de substitution.

L'écriture d'un lot de dossiers est transactionnelle : si une ligne ne peut pas être enregistrée,
aucune écriture de ce lot n'est conservée. Une erreur de collecte déjà signalée dans le bilan n'invente
pas de dossier et n'empêche pas d'enregistrer les autres lectures réussies.
Les écritures de l'instance Java sont sérialisées jusqu'à la fin de leur transaction pour protéger les
réimports simultanés. Ce n'est pas encore une coordination distribuée entre plusieurs instances de l'application.
Les erreurs de base retournent une réponse générique HTTP 503 sans identifiants de connexion.

## Installation locale du projet

PostgreSQL 16 et 17 existaient déjà et leurs services Windows restent inchangés.
Les binaires PostgreSQL 17 présents ont servi à créer un cluster dédié sous .local/postgres-data.
Il écoute uniquement sur 127.0.0.1:15432, sans créer de nouveau service Windows.
Postman continue de contacter l'application sur le port 8080 : ce sont deux ports différents.

Les bases sont séparées :

- student_career : données de l'application, conservées entre exécutions.
- student_career_test : données synthétiques des tests d'intégration.

Un compte applicatif student_career possède ces bases sans être superutilisateur ni créateur de rôles/bases.
Un compte d'initialisation distinct est limité à ce cluster de développement. Les mots de passe sont générés
et restent dans .local, exclu de Git. Ils ne sont ni affichés ni inscrits dans les scripts ou le pom.xml.
La configuration de connexion est chargée depuis .local/database.properties. Ce fichier permet de garder
IntelliJ sans saisir de mot de passe dans le chat ou les paramètres Run.
Les variables standard SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME et SPRING_DATASOURCE_PASSWORD peuvent
remplacer la configuration locale pour un autre environnement.

Le dossier .local contient les données persistantes et les identifiants de cette instance : il ne fait
pas partie du répertoire généré target. Maven clean ne l'efface pas. Un nouveau checkout sur une autre
machine doit initialiser son propre cluster ; il ne récupère pas les données ni les mots de passe via Git.
La sauvegarde/restauration et le déploiement seront préparés séparément ; persister ne remplace pas une sauvegarde.

## Commandes

Première préparation, ou vérification de l'installation existante :

```powershell
.\scripts\Initialize-Database.ps1
```

La commande est réexécutable sans effacer les données ni réinitialiser les mots de passe existants.
Si les identifiants d'un cluster existant sont absents, elle s'arrête au lieu de deviner ou remplacer un compte.

Après un redémarrage Windows, avant les tests ou un Run direct dans IntelliJ :

```powershell
.\scripts\Start-Database.ps1
```

Le lanceur `.\scripts\Start-Api.ps1` démarre aussi la base du projet si nécessaire.
Pour arrêter seulement cette instance PostgreSQL : arrêter Java, puis exécuter Stop-Database.ps1.
Ne pas arrêter les services PostgreSQL 16/17 pour utiliser ce projet.

## Tests et contrôle des données de test

108 tests réussis, zéro échec, erreur ou test ignoré après clean package.
Les tests d'intégration utilisent le profil test et student_career_test.
Un initialiseur vérifie le nom de base avant la création des composants Flyway/JPA et refuse une base
sans suffixe _test. Le nettoyage vérifie à nouveau current_database() avant toute suppression de données.
La base applicative n'est pas nettoyée par ces tests. TEST_DATABASE_URL permet une autre base de test
dédiée avec suffixe _test ; ne pas pointer les tests vers une base à conserver.

Les scénarios couvrent la migration, la lecture complète, les dates, la licence, la vérification inconnue,
le réimport et les identifiants, les doublons, les écritures concurrentes et le rollback sur ligne invalide.
Des erreurs SQL peuvent apparaître dans les logs de tests qui provoquent volontairement une contrainte ;
le résultat attendu de la suite reste BUILD SUCCESS. Aucun test n'appelle Wikidata.

## Checkpoint manuel

Suivre docs/PERSISTENCE_GUIDE.md ou la collection postman/Persistence.postman_collection.json.
Cette collection comprend une PAUSE MANUELLE après la requête 03 : arrêter Java puis le relancer avant 04.
Ne pas exécuter toute la collection automatiquement sans cet arrêt.
Le parcours complet a été exécuté avec 6 requêtes et 12 assertions sur le JAR, avec un vrai redémarrage
de Java et du cluster PostgreSQL entre les deux phases. Test-Api.ps1 a ensuite validé les routes existantes.

Le contrôle a laissé trois dossiers fictifs dans la base applicative du projet.
Les dossiers de l'ancien stockage mémoire ne sont pas transférés automatiquement : un import suffit à
les enregistrer désormais. Aucun appel Wikidata supplémentaire n'a été fait pendant cette semaine.
La validation dans IntelliJ/Postman par l'utilisateur reste attendue avant commit et push.


## Mise à jour après validation utilisateur

La persistance et le réimport des observations Wikidata ont été validés dans Postman.
Sur autorisation, les trois fixtures de la base applicative ont ensuite été supprimées : trois dossiers
WEB_OBSERVATION conservés, zéro FIXTURE. Le contenu non fictif a été comparé avant/après dans la transaction.
L'import fictif est maintenant désactivé par défaut (404 après redémarrage), activé uniquement de façon explicite.
Les tests le réactivent dans leur base dédiée. Le bilan final est de 109 tests réussis, sans échec ou erreur.
La collection de persistance a été adaptée au catalogue réel et ne réimporte plus de fixtures.
Test-Api.ps1 fonctionne en lecture seule par défaut ; -IncludeDemo exige une démo explicitement activée.
