# 🔄 WORKFLOW COMPLET — Portail de Gestion des Doctorats
> Document technique — Logique métier complète de A à Z
> Avant déploiement Docker

---

## 📌 ÉTAT ACTUEL DU PROJET

```
✅ Entities          → données pures, héritage JOINED
✅ Validators        → OCP, règles métier externalisées
✅ Repositories      → Spring Data JPA, requêtes personnalisées
✅ Services          → logique métier, couplage faible
✅ DTOs              → transfert données, sécurité
✅ Exceptions        → personnalisées, GlobalExceptionHandler
✅ Controllers Web   → Thymeleaf, formulaires
✅ Vues Thymeleaf    → frontend collaborateur intégré
✅ Config H2/PG      → profils dev/prod

🔲 Spring Security   → JWT, RBAC, protection URLs
🔲 OWASP Top 10      → protections sécurité
🔲 Tests unitaires   → JUnit, couverture 80%
🔲 Controllers REST  → API JSON
🔲 Docker            → conteneurisation
```

---

# ═══════════════════════════════════════════════════
# PARTIE 1 — SPRING SECURITY + JWT + RBAC
# ═══════════════════════════════════════════════════

## 1.1 Configuration Spring Security

### Ce qu'il faut faire :
```
config/
├── SecurityConfig.java         ← configuration principale
├── JwtConfig.java              ← configuration JWT
└── UserDetailsServiceImpl.java ← chargement user depuis BDD
```

### URLs à protéger par rôle :

```
PUBLIC (sans connexion) :
├── GET  /login
├── POST /login
├── GET  /register
├── POST /register
├── /css/**, /js/**, /images/**
└── /h2-console/** (dev uniquement)

CANDIDAT uniquement :
├── GET/POST /dossiers/nouveau
├── GET      /dossiers
├── GET/POST /publications/**
├── GET/POST /formations/**
├── GET      /soutenances/prerequis
├── POST     /soutenances/demande
├── GET/POST /derogations/nouvelle
└── GET      /dashboard/candidat

DIRECTEUR uniquement :
├── GET  /dashboard/directeur
├── GET  /dossiers/validation
├── POST /dossiers/{id}/valider
├── POST /dossiers/{id}/rejeter
├── GET  /jury/soutenance/{id}
├── GET  /jury/nouveau/{id}
└── POST /jury/nouveau

ADMIN uniquement :
├── GET      /dashboard/admin
├── GET/POST /campagnes/**
├── GET      /soutenances
├── POST     /soutenances/{id}/autoriser
├── GET/POST /soutenances/{id}/planifier
├── GET      /derogations
├── POST     /derogations/{id}/accorder
└── POST     /derogations/{id}/refuser

COMMUN (tous connectés) :
├── GET  /notifications
├── POST /notifications/**
└── GET  /soutenances/{id}
```

### Ce que SecurityConfig doit faire :
```
1. BCryptPasswordEncoder → hasher les mots de passe
2. UserDetailsService → charger user par email depuis BDD
3. Protéger les URLs selon les rôles
4. Configurer le formulaire de login Spring Security
5. Configurer le logout
6. Désactiver CSRF pour les APIs REST (/api/**)
7. Activer CSRF pour les formulaires Thymeleaf
8. Configurer les headers de sécurité
9. Gérer la redirection après login selon le rôle
```

### Redirection après login selon le rôle :
```
CANDIDAT  → /dashboard/candidat
DIRECTEUR → /dashboard/directeur
ADMIN     → /dashboard/admin
```

---

## 1.2 UserDetailsServiceImpl

### Ce qu'il faut faire :
```
1. Implémenter UserDetailsService de Spring Security
2. loadUserByUsername(email) → cherche user en BDD
3. Retourner un UserDetails avec email, motDePasse, rôle
4. Spring Security compare automatiquement le mot de passe
```

### Logique :
```
Login soumis
    │
    ▼
UserDetailsServiceImpl.loadUserByUsername(email)
    │
    ▼
userRepo.findByEmail(email)
    │
    ├── trouvé → retourne UserDetails
    └── pas trouvé → throw UsernameNotFoundException
    │
    ▼
Spring Security compare le mot de passe (BCrypt)
    │
    ├── correct → connecté → redirect selon rôle
    └── incorrect → redirect /login?error
```

---

## 1.3 JWT (pour les APIs REST)

### Ce qu'il faut faire :
```
security/jwt/
├── JwtUtils.java          ← génération + validation token
├── JwtAuthFilter.java     ← filtre HTTP pour valider le token
└── JwtAuthEntryPoint.java ← gestion erreur 401
```

### Flux JWT :
```
POST /api/auth/login
    │  { email, motDePasse }
    ▼
Vérification credentials
    │
    ▼
Génération token JWT
    │  { token: "eyJ...", role: "CANDIDAT", ... }
    ▼
Client stocke le token

Prochaine requête API :
    │  Header: Authorization: Bearer eyJ...
    ▼
JwtAuthFilter intercepte
    │
    ▼
Validation token (signature + expiration)
    │
    ├── valide → continue vers le controller
    └── invalide → 401 Unauthorized
```

### Configuration JWT :
```
jwt.secret=clé_secrète_très_longue_minimum_32_caractères
jwt.expiration=3600000      ← 1 heure en millisecondes
jwt.refresh=86400000        ← 24 heures pour refresh token
```

---

# ═══════════════════════════════════════════════════
# PARTIE 2 — LOGIQUE MÉTIER COMPLÈTE
# ═══════════════════════════════════════════════════

## 2.1 Module Inscription

### Workflow complet :

```
┌─────────────────────────────────────────────────────┐
│                  INSCRIPTION INITIALE                │
└─────────────────────────────────────────────────────┘

CANDIDAT                    DIRECTEUR               ADMIN
   │                            │                     │
   │ 1. Crée son compte         │                     │
   │    /register               │                     │
   │                            │                     │
   │ 2. Vérifie campagne ouverte│                     │
   │    Si fermée → BLOQUÉ      │                     │
   │                            │                     │
   │ 3. Remplit formulaire      │                     │
   │    - Sujet de thèse        │                     │
   │    - Directeur             │                     │
   │    - Documents (PDF/JPG)   │                     │
   │                            │                     │
   │ 4. Soumet le dossier       │                     │
   │    Statut → SOUMIS         │                     │
   │                            │                     │
   │                            │ 5. Reçoit notif     │
   │                            │    ACTION_REQUISE   │
   │                            │                     │
   │                            │ 6. Consulte dossier │
   │                            │                     │
   │                            │ 7. Valide → statut  │
   │                            │    EN_ATTENTE_ADMIN │
   │                            │    OU               │
   │                            │    Rejette → REJETE │
   │                            │                     │
   │ 8. Reçoit notif statut     │                     │
   │                            │                     │
   │                            │                     │ 9. Reçoit notif
   │                            │                     │    ACTION_REQUISE
   │                            │                     │
   │                            │                     │ 10. Valide → VALIDÉ
   │                            │                     │     OU
   │                            │                     │     Rejette → REJETÉ
   │                            │                     │
   │ 11. Reçoit notif finale    │                     │
   │     VALIDÉ ou REJETÉ       │                     │
```

### Règles de validation :
```
Avant soumission dossier :
├── Vérifier campagne OUVERTE
├── Vérifier doctorant.peutSeReinscrire() → <= 3 ans
├── Vérifier doctorant pas déjà VALIDÉ cette année
└── Vérifier documents obligatoires uploadés

Format documents acceptés :
├── PDF → diplôme, CV, lettre motivation
├── JPG/JPEG/PNG → photos
└── Taille max : 10MB par fichier

Changements de statut autorisés :
SOUMIS → EN_ATTENTE_DIRECTEUR (auto à la soumission)
EN_ATTENTE_DIRECTEUR → EN_ATTENTE_ADMIN (directeur valide)
EN_ATTENTE_DIRECTEUR → REJETE (directeur rejette)
EN_ATTENTE_ADMIN → VALIDE (admin valide)
EN_ATTENTE_ADMIN → REJETE (admin rejette)
```

---

## 2.2 Module Réinscription

### Workflow :
```
Chaque année, le doctorant doit se réinscrire

Vérifications automatiques :
├── anneeEnCours < 3 → réinscription autorisée
├── anneeEnCours >= 3 et <= 6 → vérifier dérogation accordée
└── anneeEnCours > 6 → BLOQUÉ définitivement + alerte admin

Si réinscription autorisée :
→ Même workflow que inscription initiale
→ Type campagne = REINSCRIPTION
→ anneeEnCours++ après validation
```

---

## 2.3 Module Dérogation

### Workflow :
```
CANDIDAT
   │
   │ 1. Vérifie si > 3 ans
   │    Si oui → peut demander dérogation
   │
   │ 2. Soumet demande avec motif
   │    Statut → EN_ATTENTE
   │
   ▼
ADMIN
   │
   │ 3. Reçoit notification
   │
   │ 4. Consulte la demande
   │
   ├── Accorde → StatutDerogation = ACCORDEE
   │              doctorant.peutSeReinscrire = true
   │              Notification candidat
   │
   └── Refuse → StatutDerogation = REFUSEE
                 Notification candidat

Règle : 1 seule dérogation active par doctorant à la fois
```

---

## 2.4 Module Soutenance

### Workflow complet :
```
┌─────────────────────────────────────────────────────┐
│               PROCESSUS DE SOUTENANCE                │
└─────────────────────────────────────────────────────┘

CANDIDAT
   │
   │ 1. Vérifie prérequis automatiquement
   │    ├── Publications : 2 journaux Q1/Q2 ✅/❌
   │    ├── Conférences  : 2 minimum ✅/❌
   │    └── Formation    : 200h minimum ✅/❌
   │
   │    Si prérequis non remplis → BLOQUÉ
   │    Si tous remplis → peut soumettre
   │
   │ 2. Upload documents obligatoires :
   │    ├── Demande manuscrite (PDF)
   │    ├── Rapport de thèse (PDF)
   │    ├── Rapport anti-plagiat (PDF)
   │    ├── Rapport publications/communications (PDF)
   │    ├── Attestations formations (PDF)
   │    └── Autorisation de soutenance (PDF)
   │
   │ 3. Soumet demande
   │    Statut → SOUMIS
   │
   ▼
DIRECTEUR
   │
   │ 4. Reçoit notification
   │
   │ 5. Propose membres du jury :
   │    ├── Rapporteurs (minimum 2)
   │    ├── Examinateurs
   │    └── Président (1 seul)
   │
   │ 6. Statut → EN_ATTENTE_JURY
   │
   ▼
ADMIN
   │
   │ 7. Vérifie dossier complet
   │
   │ 8. Vérifie rapports favorables
   │    Statut → EN_ATTENTE_RAPPORTS
   │
   │ 9. Donne autorisation finale
   │    Statut → AUTORISEE
   │
   │ 10. Planifie la soutenance :
   │     ├── Date
   │     ├── Heure
   │     └── Lieu
   │     Statut → PLANIFIEE
   │
   ▼
CANDIDAT + DIRECTEUR + JURY
   │
   │ 11. Reçoivent notifications :
   │     "Soutenance planifiée le XX/XX/XXXX
   │      à XXhXX — Salle XXX"
```

### Règles jury :
```
├── Minimum 2 rapporteurs
├── 1 seul président
├── Membres externes à l'établissement recommandés
└── Directeur ne peut pas être rapporteur
```

---

## 2.5 Module Publications

### Workflow :
```
CANDIDAT ajoute une publication :
├── Titre obligatoire
├── Type : JOURNAL_Q1, JOURNAL_Q2, CONFERENCE
├── Revue/Conférence
├── Année
├── URL (optionnel)
└── Statut : SOUMIS → ACCEPTE → PUBLIE

Comptage pour prérequis soutenance :
├── Journaux = count(JOURNAL_Q1) + count(JOURNAL_Q2)
└── Conférences = count(CONFERENCE)

Seuls les statuts ACCEPTE et PUBLIE comptent
pour les prérequis → à vérifier dans PublicationValidator
```

---

## 2.6 Module Formations

### Workflow :
```
CANDIDAT ajoute une formation :
├── Intitulé obligatoire
├── Nombre d'heures (min 1h)
├── Date
└── Upload attestation (PDF obligatoire)

Calcul total heures :
└── SUM(heures) WHERE doctorant = :doctorant

Prérequis : total >= 200h
```

---

## 2.7 Module Notifications

### Déclencheurs automatiques :
```
Événement                          → Destinataire    → Type
─────────────────────────────────────────────────────────────
Dossier soumis                     → Directeur       → ACTION_REQUISE
Dossier validé par directeur       → Admin           → ACTION_REQUISE
Dossier validé par admin           → Candidat        → INFO
Dossier rejeté                     → Candidat        → ALERTE
Demande soutenance soumise         → Admin           → ACTION_REQUISE
Jury proposé par directeur         → Admin           → ACTION_REQUISE
Soutenance autorisée               → Candidat        → INFO
Soutenance planifiée               → Candidat+Jury   → INFO
Dérogation accordée                → Candidat        → INFO
Dérogation refusée                 → Candidat        → ALERTE
Doctorant approche 6 ans           → Admin+Candidat  → ALERTE
Doctorant dépasse 6 ans            → Admin           → ALERTE
```

---

# ═══════════════════════════════════════════════════
# PARTIE 3 — SÉCURITÉ OWASP TOP 10
# ═══════════════════════════════════════════════════

## 3.1 A01 — Broken Access Control

```
À implémenter :
├── @PreAuthorize sur chaque méthode sensible
│   @PreAuthorize("hasRole('ADMIN')")
│   @PreAuthorize("hasRole('CANDIDAT')")
│   @PreAuthorize("hasAnyRole('DIRECTEUR','ADMIN')")
│
├── Vérifier que le candidat accède uniquement à SES données
│   if (!dossier.getDoctorant().getId().equals(doctorantId))
│       throw new AccessDeniedException("Accès refusé")
│
└── Bloquer les URLs sans authentification
    requestMatchers("/api/**").authenticated()
```

## 3.2 A02 — Cryptographic Failures

```
À implémenter :
├── BCrypt pour mots de passe (déjà fait ✅)
├── JWT signé HS256 avec clé >= 32 caractères
├── HTTPS en production (Nginx + Let's Encrypt)
├── Ne jamais logger les mots de passe
└── Variables d'environnement pour les secrets (.env)
```

## 3.3 A03 — Injection

```
À implémenter :
├── JPA uniquement → pas de SQL natif non paramétré
├── @Valid sur tous les DTOs en entrée
├── Thymeleaf échappe automatiquement (th:text) ✅
├── Valider les noms de fichiers uploadés
└── Nettoyer les caractères spéciaux dans les inputs
```

## 3.4 A04 — Insecure Design

```
À implémenter :
├── Valider prérequis côté serveur (jamais que côté client)
├── Rate limiting sur /login (max 5 tentatives/minute)
├── Bloquer compte après 5 échecs de connexion
├── Logger les actions sensibles (audit log)
└── Timeout de session (30 minutes)
```

## 3.5 A05 — Security Misconfiguration

```
À implémenter :
├── Désactiver H2 console en production
├── Headers de sécurité HTTP :
│   ├── X-Frame-Options: DENY
│   ├── X-Content-Type-Options: nosniff
│   ├── Content-Security-Policy
│   └── Strict-Transport-Security (HSTS)
├── Désactiver endpoints Actuator sensibles en prod
└── .env jamais commité sur GitHub (.gitignore)
```

## 3.6 A06 — Vulnerable Components

```
À implémenter :
├── OWASP Dependency-Check dans pom.xml
├── Trivy scan dans pipeline Docker
├── Maintenir dépendances à jour
└── SonarQube pour détecter les vulnérabilités code
```

## 3.7 A07 — Authentication Failures

```
À implémenter :
├── JWT expiration : 1h (access) + 24h (refresh)
├── Refresh token mechanism
├── Bloquer après 5 tentatives → lockoutDuration = 15min
├── Déconnexion invalide le token côté serveur
└── Mot de passe : min 8 caractères, majuscule, chiffre
```

## 3.8 A08 — Software Integrity

```
À implémenter :
├── Vérifier type MIME fichiers uploadés (pas juste extension)
├── Renommer fichiers avec UUID à l'upload
│   UUID.randomUUID() + "." + extension
├── Stocker fichiers hors répertoire web
│   /uploads/ (pas dans /static/)
├── Limiter taille : max 10MB par fichier
└── Scanner antivirus optionnel
```

## 3.9 A09 — Logging & Monitoring

```
À implémenter :
├── Logger avec SLF4J + Logback :
│   ├── Tentatives de connexion (succès + échecs)
│   ├── Changements de statut des dossiers
│   ├── Actions admin (accord/refus dérogation)
│   ├── Upload de fichiers
│   └── Erreurs 4xx et 5xx
│
├── Spring Actuator → métriques pour Prometheus
└── Grafana dashboard → alertes anomalies
```

## 3.10 A10 — SSRF

```
À implémenter :
├── Valider les URLs externes
├── Whitelist des domaines autorisés
└── Ne pas faire de requêtes HTTP depuis input utilisateur
```

---

# ═══════════════════════════════════════════════════
# PARTIE 4 — TESTS
# ═══════════════════════════════════════════════════

## 4.1 Tests unitaires (JUnit + Mockito)

```
test/
├── service/
│   ├── UserServiceImplTest.java
│   ├── DoctorantServiceImplTest.java
│   ├── DossierInscriptionServiceImplTest.java
│   ├── PublicationServiceImplTest.java
│   ├── FormationDoctoraleServiceImplTest.java
│   ├── DemandeSoutenanceServiceImplTest.java
│   └── NotificationServiceImplTest.java
│
├── validator/
│   ├── DepassementMaximalValidatorTest.java
│   ├── ReInscriptionValidatorTest.java
│   ├── PublicationValidatorTest.java
│   └── FormationValidatorTest.java
│
└── repository/
    ├── IUserRepositoryTest.java
    └── IDoctorantRepositoryTest.java
```

## 4.2 Ce qu'il faut tester :

```
UserService :
├── ✅ ajouter() → email unique
├── ✅ ajouter() → mot de passe hashé
├── ✅ findByEmail() → user trouvé
├── ✅ findByEmail() → UserNotFoundException si absent
├── ✅ modifier() → champs mis à jour
└── ✅ supprimer() → user supprimé

DoctorantService :
├── ✅ peutSeReinscrire() → true si <= 3 ans
├── ✅ peutSeReinscrire() → false si > 3 ans
├── ✅ estEnDepassement() → true si > 6 ans
└── ✅ findDoctorantsEnDepassement() → liste correcte

PublicationService :
├── ✅ prerequisPublicationsRemplis() → true si 2Q1+2conf
├── ✅ prerequisPublicationsRemplis() → false si insuffisant
└── ✅ countByDoctorantAndType() → compte correct

FormationService :
├── ✅ prerequisFormationRemplis() → true si >= 200h
├── ✅ prerequisFormationRemplis() → false si < 200h
└── ✅ getTotalHeures() → somme correcte

DemandeSoutenanceService :
├── ✅ ajouter() → bloqué si prérequis non remplis
├── ✅ ajouter() → OK si tous prérequis remplis
└── ✅ getPrerequísNonRemplis() → liste correcte

Validators :
├── ✅ DepassementMaximalValidator → règle 6 ans
├── ✅ ReInscriptionValidator → règle 3 ans
├── ✅ PublicationValidator → règle publications
└── ✅ FormationValidator → règle 200h
```

---

# ═══════════════════════════════════════════════════
# PARTIE 5 — DEVOPS
# ═══════════════════════════════════════════════════

## 5.1 Docker

### Fichiers à créer :
```
├── Dockerfile                  ← image Spring Boot
├── docker-compose.yml          ← orchestration services
├── docker-compose.dev.yml      ← override dev
├── .dockerignore               ← fichiers à ignorer
└── .env                        ← variables d'environnement
```

### Services Docker Compose :
```
services:
├── app          → Spring Boot (port 8080)
├── postgres     → PostgreSQL (port 5432)
├── sonarqube    → analyse qualité (port 9000)
├── prometheus   → métriques (port 9090)
├── grafana      → dashboards (port 3000)
└── nginx        → reverse proxy (port 80/443)
```

---

## 5.2 GitHub Actions CI/CD

### Pipeline :
```
Push sur feature/backend ou feature/frontend
    │
    ▼
1. BUILD
   └── mvn clean package -DskipTests

2. TESTS
   └── mvn test

3. ANALYSE QUALITÉ
   └── SonarQube scan

4. BUILD IMAGE DOCKER
   └── docker build -t portail-doctorat:latest .

5. SCAN SÉCURITÉ
   └── trivy image portail-doctorat:latest

6. PUSH DOCKER HUB
   └── docker push username/portail-doctorat:latest

7. DÉPLOIEMENT (sur merge dans main)
   └── ssh vers serveur + docker-compose up
```

---

## 5.3 SonarQube

```
Métriques surveillées :
├── Couverture de tests : min 80%
├── Code smells : 0 bloquants
├── Bugs : 0 critiques
├── Vulnérabilités : 0
└── Duplications : < 3%
```

---

## 5.4 Prometheus + Grafana

### Métriques Spring Boot à exposer :
```
Via Spring Actuator :
├── http_requests_total          → nombre de requêtes
├── http_request_duration        → temps de réponse
├── jvm_memory_used_bytes        → mémoire JVM
├── hikaricp_connections_active  → connexions BDD
└── process_cpu_usage            → CPU

Alertes Grafana :
├── CPU > 80% → alerte
├── Mémoire > 85% → alerte
├── Temps réponse > 2s → alerte
└── Erreurs 5xx > 10/min → alerte
```

---

## 5.5 Nginx

```
Configuration :
├── Reverse proxy → http://app:8080
├── SSL/TLS → Let's Encrypt
├── HTTP → HTTPS redirect
├── Gzip compression
├── Rate limiting → 10 req/sec par IP
└── Headers de sécurité
```

---

# ═══════════════════════════════════════════════════
# PARTIE 6 — CONTROLLERS REST API
# ═══════════════════════════════════════════════════

## 6.1 Structure :
```
controllers/rest/
├── AuthRestController.java
├── UserRestController.java
├── DoctorantRestController.java
├── CampagneRestController.java
├── DossierRestController.java
├── DocumentRestController.java
├── PublicationRestController.java
├── FormationRestController.java
├── SoutenanceRestController.java
├── JuryRestController.java
├── NotificationRestController.java
└── DerogationRestController.java
```

## 6.2 Format réponse standard :
```json
{
  "success": true,
  "data": { ... },
  "message": "Opération réussie",
  "timestamp": "2026-04-22T13:00:00"
}
```

## 6.3 Sécurité REST :
```
Chaque endpoint REST :
├── Vérifie le token JWT dans le header Authorization
├── Vérifie le rôle avec @PreAuthorize
└── Retourne 401 si token invalide
    Retourne 403 si rôle insuffisant
```

---

# ═══════════════════════════════════════════════════
# ORDRE DE RÉALISATION RECOMMANDÉ
# ═══════════════════════════════════════════════════

```
ÉTAPE 1 — Spring Security + RBAC
├── UserDetailsServiceImpl
├── SecurityConfig (URLs protégées par rôle)
├── Redirection après login selon rôle
└── Test : login/logout fonctionne

ÉTAPE 2 — JWT (pour REST API)
├── JwtUtils
├── JwtAuthFilter
├── JwtAuthEntryPoint
└── Test : token généré et validé

ÉTAPE 3 — OWASP Top 10
├── @PreAuthorize sur les méthodes
├── Rate limiting
├── Validation fichiers uploadés
├── Headers de sécurité
└── Variables d'environnement

ÉTAPE 4 — Tests unitaires
├── Tests services
├── Tests validators
├── Tests repositories
└── Couverture >= 80%

ÉTAPE 5 — Controllers REST
├── AuthRestController (login JWT)
├── Tous les controllers REST
└── Test avec Postman

ÉTAPE 6 — Docker
├── Dockerfile
├── docker-compose.yml
└── Test : app démarre dans Docker

ÉTAPE 7 — GitHub Actions
├── Pipeline CI (build + tests)
├── SonarQube integration
├── Trivy scan
└── Pipeline CD (deploy)

ÉTAPE 8 — Monitoring
├── Prometheus config
├── Grafana dashboards
└── Alertes configurées

ÉTAPE 9 — Nginx
├── Configuration reverse proxy
├── SSL Let's Encrypt
└── Headers sécurité

ÉTAPE 10 — Déploiement final
├── VPS / Railway / Heroku
├── Variables d'environnement prod
└── Test end-to-end complet
```

---

# ═══════════════════════════════════════════════════
# CHECKLIST FINALE AVANT DÉPLOIEMENT
# ═══════════════════════════════════════════════════

```
SÉCURITÉ
├── [ ] Spring Security configuré et testé
├── [ ] JWT fonctionnel
├── [ ] RBAC appliqué sur toutes les URLs
├── [ ] BCrypt sur les mots de passe
├── [ ] CSRF activé pour Thymeleaf
├── [ ] CORS configuré pour REST
├── [ ] Rate limiting activé
├── [ ] Headers HTTP sécurité configurés
├── [ ] HTTPS configuré (Nginx)
├── [ ] Secrets dans .env (jamais dans le code)
├── [ ] H2 console désactivée en prod
└── [ ] OWASP Dependency-Check passé

QUALITÉ CODE
├── [ ] Tests unitaires écrits
├── [ ] Couverture >= 80%
├── [ ] SonarQube : 0 bugs critiques
├── [ ] SonarQube : 0 vulnérabilités
└── [ ] Code review effectuée

FONCTIONNEL
├── [ ] Inscription fonctionne
├── [ ] Réinscription fonctionne
├── [ ] Règle 3 ans vérifiée
├── [ ] Règle 6 ans vérifiée
├── [ ] Dérogation fonctionne
├── [ ] Publications ajoutées
├── [ ] Formations ajoutées
├── [ ] Prérequis soutenance vérifiés
├── [ ] Demande soutenance soumise
├── [ ] Jury proposé par directeur
├── [ ] Soutenance autorisée par admin
├── [ ] Soutenance planifiée
├── [ ] Notifications envoyées
└── [ ] Login/logout fonctionne

DEVOPS
├── [ ] Docker build réussi
├── [ ] docker-compose up fonctionne
├── [ ] Pipeline GitHub Actions passe
├── [ ] Trivy : 0 vulnérabilités critiques
├── [ ] Prometheus métriques collectées
├── [ ] Grafana dashboards configurés
└── [ ] Nginx reverse proxy fonctionnel
```
