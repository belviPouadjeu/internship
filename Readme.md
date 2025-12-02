# November -2025

## - Intégration des Frais de Chéquier

## Contexte
Intégration d'un système de calcul de frais dynamique pour les chéquiers, remplaçant les frais statiques stockés en 
base de données par un appel à un service dédié.

## Difficultés Rencontrées et Solutions

### 1. Conflit d'Énumérations OperationType
**Problème :** Plusieurs énumérations `OperationType` existaient dans différents modules, causant des conflits d'imports.

**Solution :** 
- Renommé l'énumération dans `internal-client-common` en `FeesOperationType`
- Créé une nouvelle énumération avec 12 types d'opérations incluant `CHECKBOOK`
- Mis à jour tous les imports et références

### 2. Compilation Maven et Dépendances
**Problème :** Les nouvelles classes n'étaient pas disponibles dans les projets dépendants.

**Solution :**
- Exécution de `mvn clean install` sur `pkf-ib-be-common-lib`
- Mise à jour des dépendances dans les projets consommateurs

### 3. Configuration Spring Boot
**Problème :** Erreurs de démarrage des services avec des beans manquants.

**Solution :**
- Ajout des annotations `@ComponentScan` appropriées
- Configuration des packages de scan pour inclure les clients internes

### 4. Problèmes d'Authentification JWT
**Problème :** Erreurs 401/403 lors des appels aux endpoints internes.

**Solution :**
- Création d'un `TestTokenController` pour générer des tokens JWT valides
- Configuration des permissions requises (`USER_READ`, `USER_CREATE`)
- Utilisation d'endpoints internes (`/api/internal/`) pour la communication service-à-service

### 5. Contraintes de Base de Données
**Problème :** Violations de contraintes NOT NULL lors des tests.

**Solution :**
- Création de données de test complètes incluant :
  - Utilisateurs avec tous les champs requis
  - Comptes avec `is_auto_charging = false`
  - Souscriptions avec `package_id` valide
  - Banques, branches et devises
  - Relations `users_accounts` appropriées

### 6. Mapping JSON et Validation
**Problème :** Erreurs de sérialisation/désérialisation JSON.

**Solution :**
- Utilisation de `camelCase` pour les champs JSON (`accountId`, `userId`)
- Ajout de validation `@DecimalMin("0.01")` sur le champ `amount`
- Structure de réponse simplifiée avec un seul champ `operationFee`

### 7. Gestion d'Erreurs et Internationalisation
**Problème :** Messages d'erreur non localisés pour les échecs de calcul de frais.

**Solution :**
- Ajout de `FEE_CALCULATION_FAILED` dans `ErrorCode`
- Mise à jour des fichiers de localisation (en, fr, pt)
- Implémentation de try-catch avec gestion d'erreurs appropriée

### 8. Erreurs CBS Adapter
**Problème :** L'appel au CBS adapter causait des erreurs serveur 500.

**Solution Temporaire :**
- Modification de `getOperationFeesForClient()` pour retourner des frais fixes
- Remplacement de l'appel CBS par `return new OperationFeesResponse(BigDecimal.valueOf(15.75));`
- Permet de tester l'intégration complète sans dépendre du CBS

## Architecture Finale

### Rôle du CBS (Core Banking System)
**CBS (Core Banking System) :**
- Système bancaire central qui gère toutes les opérations bancaires
- Calcule les frais réels selon les règles métier de la banque
- Source de vérité pour les tarifications et politiques bancaires
- Système externe critique pour les calculs de frais précis

**CBS Adapter Client** (`@cbs-adapter-client`) :
- Client pour communiquer avec le système bancaire central (CBS)
- Contient les utilitaires pour accéder au CBS via le service adapter
- Gère les appels HTTP vers les endpoints CBS
- Inclut `AdapterClient` avec `operationFeeClient.calculateOperationFees()`

**CBS Adapter Model** (`@cbs-adapter-model`) :
- Modèles de requête/réponse pour les appels CBS
- Contient `CalculateOperationFeesAdapterRequest` et `CalculateOperationFeeAdapterResponse`
- Définit l'énumération `OperationType` pour les opérations CBS
- Structure les données échangées avec le système bancaire

**Flux CBS Normal :**
1. Payment-service → CBS Adapter Client → CBS
2. CBS calcule les frais réels basés sur les règles bancaires
3. CBS → CBS Adapter → Payment-service

**Problème CBS :** Erreurs serveur 500 lors des appels, d'où la solution temporaire avec frais fixes.

### Services Impliqués
- **user-service** : Consomme les frais via `FeesAmountInternalClient`
- **payment-service** : Fournit l'endpoint `/api/internal/v1/payment/fees`
- **common-lib** : Contient les DTOs et clients partagés
- **cbs-adapter** : Interface avec le système bancaire central

### Flux de Données
1. User-service appelle `FeesAmountInternalClient.calculateFees()`
2. Client fait un POST vers `/api/internal/v1/payment/fees`
3. Payment-service retourne `{"operationFee": 15.75}`
4. User-service utilise les frais pour la logique métier

## Test Postman Validé
`POST /api/internal/v1/payment/fees`
```json
{
    "accountId": "284e988d-4041-491e-a69a-27cb0da49fb6",
    "userId": "fd4cc7a3-29fb-4d9b-821b-27204aac6220",
    "operationType": "CHECKBOOK",
    "amount": 100.00
}

```

Response: `{"operationFee": 15.75}`

## Compétences Acquises

### Architecture Microservices
- **Communication inter-services** : Implémentation de clients internes pour la communication service-à-service
- **Séparation des responsabilités** : Distinction entre services métier (user, payment) et services techniques (adapter)
- **Gestion des dépendances** : Utilisation de bibliothèques communes pour partager DTOs et clients

### Spring Boot & Maven
- **Configuration Spring** : Maîtrise des annotations `@ComponentScan`, `@FeignClient`, `@Service`
- **Gestion des beans** : Résolution des conflits de dépendances et configuration des packages
- **Build Maven** : Compilation multi-modules avec `mvn clean install`

### Gestion des Données
- **Modélisation JPA** : Entités avec relations complexes (OneToMany, ManyToOne)
- **Contraintes de base** : Gestion des NOT NULL, clés étrangères, et intégrité référentielle
- **Données de test** : Création de jeux de données cohérents pour les tests

### Sécurité & Authentification
- **JWT Tokens** : Génération et validation de tokens pour l'authentification
- **Permissions** : Configuration des rôles et permissions (`USER_READ`, `USER_CREATE`)
- **Endpoints internes** : Sécurisation des communications service-à-service

### Gestion d'Erreurs
- **Exception handling** : Try-catch avec exceptions métier personnalisées
- **Internationalisation** : Messages d'erreur multilingues (en, fr, pt)
- **Codes d'erreur** : Standardisation avec enum `ErrorCode`

### Intégration & Tests
- **Tests Postman** : Validation des endpoints avec données réelles
- **Débogage** : Identification et résolution d'erreurs complexes (500, 401, contraintes DB)
- **Solutions temporaires** : Contournement d'intégrations externes défaillantes
- **Intégration CBS** : Compréhension du rôle du Core Banking System dans l'écosystème bancaire

### Modélisation & Design
- **DTOs** : Conception de structures de données pour APIs REST
- **Validation** : Annotations de validation (`@DecimalMin`, `@NotNull`)
- **Enum management** : Gestion des conflits d'énumérations entre modules

## CBS Integration

### Core Banking System (CBS)
The CBS is the central banking system that manages all banking operations and calculates real fees based on bank business rules. 
It serves as the source of truth for pricing and banking policies.

### CBS Adapter Components
- **CBS Adapter Client** (`@cbs-adapter-client`): Client utilities to communicate with CBS via adapter service
- **CBS Adapter Model** (`@cbs-adapter-model`): Request/response models for CBS adapter calls

### Integration Flow
1. Payment Service → CBS Adapter Client → CBS
2. CBS calculates real fees based on banking rules
3. CBS → CBS Adapter → Payment Service


## Prochaines Étapes
1. Restaurer l'intégration CBS quand le service sera stable


1. 🌍 IBAN (International Bank Account Number)L'IBAN n'a pas de longueur fixe, mais il possède une longueur minimale et maximale définie par la norme ISO 13616.CaractéristiqueValeurLongueur Minimale15 caractèresLongueur Maximale34 caractèresStructure : L'IBAN est composé de lettres majuscules et de chiffres.2 lettres pour le code pays (ex : FR pour la France).2 chiffres pour la clé de contrôle (la validation Modulo 97).Jusqu'à 30 caractères pour le BBAN (Basic Bank Account Number), qui est le numéro de compte national.Exemple pour la France : L'IBAN français a une longueur fixe de 27 caractères (FR + 2 chiffres + 23 chiffres/lettres du BBAN).2. 🇫🇷 RIB (Relevé d'Identité Bancaire)Le RIB est un format national, utilisé principalement en France. Il est toujours composé de 23 chiffres et/ou lettres pour l'identifiant du compte lui-même, mais il est souvent présenté avec 5 chiffres supplémentaires (Code Banque et Code Guichet).Longueur Totale (Format Standard) : 23 caractères.5 chiffres : Code Banque5 chiffres : Code Guichet11 chiffres/lettres : Numéro de Compte2 chiffres : Clé RIB (Clé de contrôle du RIB)Si l'on inclut le code banque et le code guichet, le numéro de compte national (BBAN) qui compose l'IBAN est de 23 caractères.Clé à retenir : Un RIB est un format fixe de 23 caractères, tandis que l'IBAN a une longueur variable selon le pays (maximum 34).
