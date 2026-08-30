# Gestion des Stagiaires

Application Spring Boot + Thymeleaf + Bootstrap pour le suivi des stagiaires (sujet,
livrables, avancement, évaluations), avec deux espaces : responsable et stagiaire.

## Lancer le projet

Aucune installation de base de données nécessaire pour l'instant (H2 embarquée) :

```
./mvnw spring-boot:run
```

Puis ouvrez `http://localhost:8080/login`.

Comptes de démo créés automatiquement au premier lancement (voir aussi les logs de démarrage) :

| Utilisateur | Mot de passe | Rôle |
|---|---|---|
| responsable1 | Responsable#123 | RESPONSABLE (a déjà 1 stagiaire assigné) |
| stagiaire1 | Stagiaire#123 | STAGIAIRE (a un sujet avec 3 livrables + 1 évaluation) |
| stagiaire2 | Stagiaire#123 | STAGIAIRE (sans sujet - pour tester l'assignation) |

Désactivez ce jeu de données de démo une fois vos vraies données en place, dans
`application.properties` : `app.seed-demo-data=false`.

## Architecture

- **Sécurité** : Spring Security classique, session + formulaire de connexion (pas de JWT -
  inutile ici puisqu'il n'y a pas d'API séparée, tout est rendu côté serveur par Thymeleaf).
  Un stagiaire ne peut agir que sur ses propres livrables (vérifié dans `LivrableService`),
  un responsable ne peut voir/modifier que les sujets dont il est responsable (vérifié dans
  `SujetService.verifierProprietaireResponsable`). C'est ce qui répond à vos besoins non
  fonctionnels 1 et 2.
- **Modèle de données** : `Utilisateur` (rôle STAGIAIRE ou RESPONSABLE) → `Sujet` (1 sujet =
  1 stagiaire + 1 responsable) → `Livrable` (plusieurs par sujet, avancement 0-100%) et
  `Evaluation` (plusieurs par sujet, intermédiaire ou finale).
- **Mots de passe** : BCrypt, jamais stockés en clair.

## Passer de H2 à Oracle (Semaine 3)

1. Démarrez Oracle Database Free en conteneur (voir la doc Oracle Container Registry).
2. Dans `pom.xml`, décommentez la dépendance `ojdbc11`.
3. Dans `application.properties`, remplacez les 4 lignes `spring.datasource.*` par :

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=stagemgmt
spring.datasource.password=votre_mot_de_passe
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
```

Rien d'autre à changer - les entités JPA et le code restent identiques, c'est tout
l'intérêt d'utiliser Spring Data JPA plutôt que du SQL écrit à la main.

## Prochaines étapes suggérées (au-delà du cœur applicatif)

- Semaine 3 : bascule Oracle + quelques requêtes SQL manuelles pour vous entraîner
  (même si JPA génère le SQL, comprendre ce qu'il génère est le but pédagogique)
- Semaine 4 : `Dockerfile` pour l'appli, Jenkins en conteneur, premier `Jenkinsfile`
  (stages build + test)
- Semaine 5 : ajout des stages SonarQube, build image Docker, déploiement automatique

Dites-moi quand vous voulez attaquer le Dockerfile/Jenkinsfile et on avancera pareil,
étape par étape.
