# LOGIQUE METIER ET REGLES DE GESTION COMPLETES
# Portail de Gestion des Doctorats - Spring Boot
# Document destine a GitHub Copilot pour implementation directe

---

## CONTEXTE TECHNIQUE

```
Framework    : Spring Boot 4.0.3
Langage      : Java 17
Securite     : Spring Security + JWT + BCrypt
Base de donnees : PostgreSQL (prod) / H2 (dev)
ORM          : Spring Data JPA / Hibernate
Template     : Thymeleaf
Package base : org.example.doctoratapp
```

---

## ENTITES DISPONIBLES

```
User { id, nom, prenom, email, motDePasse, role(CANDIDAT/DIRECTEUR/ADMIN), dateDeCreation }
Doctorant extends User { numInscription, dateInscriptionInitiale, anneeEnCours, statutDoctorant(ACTIF/SUSPENDU/DIPLOME) }
DirecteurThese extends User { grade, laboratoire, specialite }
CampagneInscription { id, dateOuverture, dateFermeture, anneeUniversitaire, type(INSCRIPTION/REINSCRIPTION), statut(OUVERTE/FERMEE) }
DossierInscription { id, sujetThese, dateDepot, statut(SOUMIS/EN_ATTENTE_DIRECTEUR/EN_ATTENTE_ADMIN/VALIDE/REJETE), commentaire, doctorant, directeurThese, campagne }
Document { id, typeDocument, nomFichier, cheminFichier, format, dateDepot, dossierInscription, demandeSoutenance, formation }
Publication { id, titre, type(JOURNAL_Q1/JOURNAL_Q2/CONFERENCE), revue, annee, url, statut(SOUMIS/ACCEPTE/PUBLIE), doctorant }
FormationDoctorale { id, intitule, heures, dateFormation, doctorant, attestation }
DemandeSoutenance { id, dateDepot, statut(SOUMIS/PREREQUIS_VALIDES/EN_ATTENTE_JURY/EN_ATTENTE_RAPPORTS/AUTORISEE/PLANIFIEE/REJETEE), commentaire, doctorant }
MembreJury { id, nom, prenom, grade, etablissement, role(RAPPORTEUR/EXAMINATEUR/PRESIDENT), demandeSoutenance }
Soutenance { id, dateSoutenance, heure, lieu, autorisationAdmin, mention, demandeSoutenance }
Notification { id, message, dateEnvoi, lu, type(INFO/ALERTE/ACTION_REQUISE), lienCible, destinataire }
Derogation { id, motif, dateDemande, statut(EN_ATTENTE/ACCORDEE/REFUSEE), doctorant, accordeePar }
```

---

# MODULE 1 - GESTION DES UTILISATEURS

## REGLE US-01 : Unicite de l'email
```
QUAND : un utilisateur s'inscrit ou est cree
VERIFIER : qu'aucun autre utilisateur n'a le meme email
SI email existe -> lancer EmailDejaUtiliseException("Email deja utilise : " + email)
SI email n'existe pas -> autoriser la creation
IMPLEMENTATION : userRepo.existsByEmail(email) dans UserServiceImpl.ajouter()
```

## REGLE US-02 : Hashage du mot de passe
```
QUAND : un utilisateur est cree ou modifie son mot de passe
TOUJOURS : hasher le mot de passe avec BCrypt avant la sauvegarde
JAMAIS : stocker le mot de passe en clair
CODE : user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()))
IMPLEMENTATION : dans UserServiceImpl.ajouter() et UserServiceImpl.modifier()
```

## REGLE US-03 : Modification mot de passe conditionnelle
```
QUAND : un utilisateur modifie son profil
SI motDePasse fourni et non vide -> hasher et mettre a jour
SI motDePasse null ou vide -> garder l'ancien mot de passe
IMPLEMENTATION : dans UserServiceImpl.modifier()
```

## REGLE US-04 : Role par defaut a l'inscription
```
QUAND : un candidat cree un compte via /register
ATTRIBUER : role CANDIDAT par defaut
ADMIN et DIRECTEUR : crees uniquement par l'administrateur
IMPLEMENTATION : dans AuthWebController.register() -> dto.setRole("CANDIDAT")
```

## REGLE US-05 : Confirmation mot de passe
```
QUAND : un utilisateur s'inscrit
VERIFIER : motDePasse == confirmMotDePasse
SI differents -> retourner formulaire avec message "Les mots de passe ne correspondent pas"
IMPLEMENTATION : dans AuthWebController.register()
```

## REGLE US-06 : Chargement UserDetails pour Spring Security
```
QUAND : Spring Security authentifie un utilisateur
CHARGER : le user depuis la BDD par email
RETOURNER : UserDetails avec email, motDePasse hache, role
LANCER : UsernameNotFoundException si email introuvable
IMPLEMENTATION : dans UserDetailsServiceImpl.loadUserByUsername(email)
```

## REGLE US-07 : Redirection apres login selon le role
```
QUAND : un utilisateur se connecte avec succes
SI role == CANDIDAT  -> rediriger vers /dashboard/candidat
SI role == DIRECTEUR -> rediriger vers /dashboard/directeur
SI role == ADMIN     -> rediriger vers /dashboard/admin
IMPLEMENTATION : dans CustomAuthenticationSuccessHandler
```

---

# MODULE 2 - GESTION DES CAMPAGNES D'INSCRIPTION

## REGLE CA-01 : Dates coherentes
```
QUAND : l'admin cree ou modifie une campagne
VERIFIER : dateFermeture > dateOuverture
SI non -> lancer exception "La date de fermeture doit etre apres la date d'ouverture"
IMPLEMENTATION : dans CampagneInscriptionServiceImpl.ajouter() et modifier()
```

## REGLE CA-02 : Une seule campagne active par type et par annee
```
QUAND : l'admin cree une campagne INSCRIPTION ou REINSCRIPTION
VERIFIER : pas de campagne OUVERTE du meme type pour la meme anneeUniversitaire
SI existe -> lancer exception "Une campagne de ce type est deja ouverte pour cette annee"
IMPLEMENTATION : dans CampagneInscriptionServiceImpl.ajouter()
```

## REGLE CA-03 : Fermeture automatique des campagnes
```
QUAND : chaque nuit a minuit (cron = "0 0 0 * * *")
FAIRE : trouver campagnes OUVERTE dont dateFermeture < LocalDate.now()
FAIRE : passer leur statut a FERMEE
IMPLEMENTATION : @Scheduled dans ScheduledTaskService.fermerCampagnesExpirees()
```

## REGLE CA-04 : Impossible de supprimer une campagne avec des dossiers
```
QUAND : l'admin supprime une campagne
VERIFIER : aucun dossier associe a cette campagne
SI dossiers existent -> lancer exception "Impossible de supprimer une campagne avec des dossiers"
IMPLEMENTATION : dans CampagneInscriptionServiceImpl.supprimer()
```

---

# MODULE 3 - GESTION DES DOSSIERS D'INSCRIPTION

## REGLE DO-01 : Campagne ouverte obligatoire
```
QUAND : un doctorant soumet un dossier
VERIFIER : une campagne du bon type est OUVERTE
SI aucune campagne ouverte -> lancer CampagneFermeeException
IMPLEMENTATION : dans DossierInscriptionServiceImpl.ajouter()
-> campagneService.findCampagneActive(anneeUniversitaire)
```

## REGLE DO-02 : Reinscription bloquee apres 3 ans
```
QUAND : un doctorant soumet un dossier de REINSCRIPTION
CALCULER : nombreAnnees = ChronoUnit.YEARS.between(dateInscriptionInitiale, LocalDate.now())
SI nombreAnnees > 3 ET aucune derogation ACCORDEE -> lancer ReInscriptionNonAutoriseeException
SI nombreAnnees > 3 ET derogation ACCORDEE -> autoriser
IMPLEMENTATION : dans DossierInscriptionServiceImpl.ajouter()
-> doctorantService.peutSeReinscrire(doctorant)
```

## REGLE DO-03 : Un seul dossier actif a la fois
```
QUAND : un doctorant soumet un dossier
VERIFIER : pas de dossier SOUMIS, EN_ATTENTE_DIRECTEUR ou EN_ATTENTE_ADMIN existant
SI dossier actif existe -> lancer exception "Vous avez deja un dossier en cours de traitement"
IMPLEMENTATION : dans DossierInscriptionServiceImpl.ajouter()
-> dossierRepo.findByDoctorantAndStatutIn(doctorant, [SOUMIS, EN_ATTENTE_DIRECTEUR, EN_ATTENTE_ADMIN])
```

## REGLE DO-04 : Statut initial a la soumission
```
QUAND : un dossier est soumis
DEFINIR : statut = SOUMIS
DEFINIR : dateDepot = LocalDate.now()
ENSUITE : envoyer notification au directeur de these
IMPLEMENTATION : dans DossierInscriptionServiceImpl.ajouter()
```

## REGLE DO-05 : Notification au directeur apres soumission
```
QUAND : un dossier passe au statut SOUMIS
ENVOYER : notification au directeur de these
TYPE : ACTION_REQUISE
MESSAGE : "Nouveau dossier de [NomDoctorant] en attente de votre validation"
LIEN : "/dossiers/" + dossier.getId()
IMPLEMENTATION : dans DossierInscriptionServiceImpl.ajouter()
```

## REGLE DO-06 : Validation par le directeur
```
QUAND : le directeur valide un dossier EN_ATTENTE_DIRECTEUR
CHANGER : statut -> EN_ATTENTE_ADMIN
ENVOYER : notification a l'admin
TYPE : ACTION_REQUISE
MESSAGE : "Dossier de [NomDoctorant] valide par le directeur, en attente de votre validation"
IMPLEMENTATION : dans DossierInscriptionServiceImpl.changerStatut()
```

## REGLE DO-07 : Rejet par le directeur
```
QUAND : le directeur rejette un dossier
VERIFIER : commentaire non vide (obligatoire)
CHANGER : statut -> REJETE
ENVOYER : notification au doctorant
TYPE : ALERTE
MESSAGE : "Votre dossier a ete rejete par votre directeur : [commentaire]"
IMPLEMENTATION : dans DossierInscriptionServiceImpl.changerStatut()
```

## REGLE DO-08 : Validation finale par l'admin
```
QUAND : l'admin valide un dossier EN_ATTENTE_ADMIN
CHANGER : statut -> VALIDE
INCREMENTER : doctorant.anneeEnCours++
ENVOYER : notification au doctorant
TYPE : INFO
MESSAGE : "Votre dossier a ete valide. Bonne continuation dans votre parcours doctoral."
IMPLEMENTATION : dans DossierInscriptionServiceImpl.changerStatut()
```

## REGLE DO-09 : Rejet par l'admin
```
QUAND : l'admin rejette un dossier
VERIFIER : commentaire non vide
CHANGER : statut -> REJETE
ENVOYER : notification au doctorant
TYPE : ALERTE
MESSAGE : "Votre dossier a ete rejete par l'administration : [commentaire]"
IMPLEMENTATION : dans DossierInscriptionServiceImpl.changerStatut()
```

## REGLE DO-10 : Documents obligatoires inscription
```
QUAND : un doctorant soumet un dossier d'inscription
VERIFIER : presence de ces documents
- DIPLOME (obligatoire)
- CV (obligatoire)
- LETTRE_MOTIVATION (obligatoire)
SI document manquant -> lancer exception "Document manquant : [typeDocument]"
IMPLEMENTATION : dans DossierInscriptionServiceImpl.verifierDocumentsObligatoires()
```

## REGLE DO-11 : Format et taille des fichiers
```
QUAND : un document est uploade
VERIFIER : format dans [PDF, JPG, JPEG, PNG]
VERIFIER : taille <= 10MB
VERIFIER : type MIME reel (pas seulement l'extension)
RENOMMER : UUID.randomUUID() + "." + extension
STOCKER : dans /uploads/ (hors repertoire web)
SI format invalide -> lancer DocumentFormatInvalideException(format)
IMPLEMENTATION : dans DocumentServiceImpl.ajouter()
```

---

# MODULE 4 - GESTION DE LA DUREE DU DOCTORAT

## REGLE DU-01 : Calcul duree doctorat
```
FORMULE : nombreAnnees = ChronoUnit.YEARS.between(dateInscriptionInitiale, LocalDate.now())
UTILISER : dans tous les services qui verifient la duree
IMPLEMENTATION : dans DoctorantServiceImpl
```

## REGLE DU-02 : Alerte approche 6 ans
```
QUAND : nombreAnnees >= 5
ENVOYER : notification au doctorant
TYPE : ALERTE
MESSAGE : "Attention : vous approchez de la duree maximale du doctorat (6 ans)"
ENVOYER : notification a l'admin
TYPE : ALERTE
MESSAGE : "[NomDoctorant] approche de la duree maximale du doctorat"
IMPLEMENTATION : @Scheduled(cron = "0 0 8 * * MON") dans ScheduledTaskService
```

## REGLE DU-03 : Blocage a 6 ans
```
QUAND : nombreAnnees >= 6
CHANGER : doctorant.statutDoctorant -> SUSPENDU
ENVOYER : notification urgente a l'admin
TYPE : ALERTE
MESSAGE : "[NomDoctorant] a depasse la duree maximale de 6 ans. Action requise."
BLOQUER : toute reinscription meme avec derogation
IMPLEMENTATION : dans DoctorantServiceImpl.estEnDepassement()
-> verifier dans DossierInscriptionServiceImpl.ajouter()
```

## REGLE DU-04 : Trouver doctorants en depassement
```
QUAND : l'admin consulte le dashboard
AFFICHER : doctorants dont dateInscriptionInitiale < LocalDate.now().minusYears(6)
IMPLEMENTATION : dans DoctorantServiceImpl.findDoctorantsEnDepassement()
-> doctorantRepo.findByDateInscriptionInitialeBefore(LocalDate.now().minusYears(6))
```

---

# MODULE 5 - GESTION DES DEROGATIONS

## REGLE DER-01 : Conditions pour demander une derogation
```
QUAND : un doctorant demande une derogation
VERIFIER : nombreAnnees > 3 (sinon pas besoin)
VERIFIER : nombreAnnees < 6 (au-dela bloque definitivement)
VERIFIER : pas de derogation EN_ATTENTE ou ACCORDEE existante
SI conditions non remplies -> lancer exception appropriee
IMPLEMENTATION : dans DerogationServiceImpl.ajouter()
```

## REGLE DER-02 : Une seule derogation active a la fois
```
QUAND : un doctorant soumet une demande de derogation
VERIFIER : pas de derogation EN_ATTENTE pour ce doctorant
SI existe -> lancer exception "Vous avez deja une demande de derogation en cours"
IMPLEMENTATION : dans DerogationServiceImpl.ajouter()
-> derogationRepo.findByDoctorantAndStatut(doctorant, EN_ATTENTE)
```

## REGLE DER-03 : Statut initial derogation
```
QUAND : une derogation est soumise
DEFINIR : statut = EN_ATTENTE
DEFINIR : dateDemande = LocalDate.now()
ENVOYER : notification a l'admin
TYPE : ACTION_REQUISE
MESSAGE : "[NomDoctorant] demande une derogation pour depassement des 3 ans"
IMPLEMENTATION : dans DerogationServiceImpl.ajouter()
```

## REGLE DER-04 : Accord de derogation par l'admin
```
QUAND : l'admin accorde une derogation
CHANGER : statut -> ACCORDEE
DEFINIR : accordeePar = admin connecte
ENVOYER : notification au doctorant
TYPE : INFO
MESSAGE : "Votre demande de derogation a ete accordee. Vous pouvez vous reinscrire."
IMPLEMENTATION : dans DerogationServiceImpl.accorder(id, admin)
```

## REGLE DER-05 : Refus de derogation par l'admin
```
QUAND : l'admin refuse une derogation
CHANGER : statut -> REFUSEE
DEFINIR : accordeePar = admin connecte
ENVOYER : notification au doctorant
TYPE : ALERTE
MESSAGE : "Votre demande de derogation a ete refusee."
IMPLEMENTATION : dans DerogationServiceImpl.refuser(id, admin)
```

---

# MODULE 6 - GESTION DES PUBLICATIONS

## REGLE PU-01 : Types de publications reconnus
```
JOURNAL_Q1  -> article dans revue de rang Q1
JOURNAL_Q2  -> article dans revue de rang Q2
CONFERENCE  -> communication dans conference internationale
Seuls ces 3 types comptent pour les prerequis de soutenance
```

## REGLE PU-02 : Statuts des publications
```
SOUMIS  -> soumis a la revue (NE COMPTE PAS pour prerequis)
ACCEPTE -> accepte mais pas encore publie (COMPTE pour prerequis)
PUBLIE  -> publie officiellement (COMPTE pour prerequis)
SEULS ACCEPTE et PUBLIE comptent pour les prerequis de soutenance
IMPLEMENTATION : dans PublicationValidator.isValid()
-> filter par statut IN [ACCEPTE, PUBLIE]
```

## REGLE PU-03 : Calcul prerequis publications
```
FORMULE :
journauxQ1Q2 = count(publications WHERE type IN [JOURNAL_Q1, JOURNAL_Q2]
               AND statut IN [ACCEPTE, PUBLIE])
conferences  = count(publications WHERE type = CONFERENCE
               AND statut IN [ACCEPTE, PUBLIE])
PREREQUIS REMPLIS SI : journauxQ1Q2 >= 2 ET conferences >= 2
IMPLEMENTATION : dans PublicationServiceImpl.prerequisPublicationsRemplis()
```

## REGLE PU-04 : Publication appartient au doctorant
```
QUAND : un doctorant modifie ou supprime une publication
VERIFIER : publication.doctorant.email == principal.getName()
SI non -> lancer AccessDeniedException
IMPLEMENTATION : dans PublicationServiceImpl.modifier() et supprimer()
```

---

# MODULE 7 - GESTION DES FORMATIONS DOCTORALES

## REGLE FO-01 : Calcul total heures
```
FORMULE : totalHeures = SUM(heures) WHERE doctorant = :doctorant
IMPLEMENTATION : formationRepo.sumHeuresByDoctorant(doctorant)
@Query("SELECT COALESCE(SUM(f.heures), 0) FROM FormationDoctorale f WHERE f.doctorant = :doctorant")
```

## REGLE FO-02 : Prerequis formation
```
PREREQUIS REMPLIS SI : totalHeures >= 200
IMPLEMENTATION : dans FormationDoctoraleServiceImpl.prerequisFormationRemplis()
```

## REGLE FO-03 : Attestation obligatoire
```
QUAND : un doctorant ajoute une formation
VERIFIER : une attestation PDF est fournie
SI attestation manquante -> lancer exception "L'attestation de formation est obligatoire"
IMPLEMENTATION : dans FormationDoctoraleServiceImpl.ajouter()
```

## REGLE FO-04 : Heures positives
```
QUAND : un doctorant ajoute une formation
VERIFIER : heures >= 1
SI heures <= 0 -> lancer exception "Le nombre d'heures doit etre superieur a 0"
IMPLEMENTATION : @Min(1) dans FormationDoctoraleDTO
```

---

# MODULE 8 - GESTION DE LA SOUTENANCE

## REGLE SO-01 : Verification globale des prerequis
```
QUAND : un doctorant soumet une demande de soutenance
VERIFIER TOUS LES PREREQUIS :
1. journauxQ1Q2 >= 2 (statut ACCEPTE ou PUBLIE)
2. conferences >= 2 (statut ACCEPTE ou PUBLIE)
3. totalHeuresFormation >= 200
4. dossierInscription.statut == VALIDE
5. doctorant.statutDoctorant == ACTIF
6. nombreAnnees < 6 (pas en depassement)
SI un prerequis manque -> lancer PrerequísNonRemplisException(listeManquants)
RETOURNER : liste detaillee des prerequis non remplis
IMPLEMENTATION : dans DemandeSoutenanceServiceImpl.verifierTousPrerequis()
```

## REGLE SO-02 : Statut initial demande soutenance
```
QUAND : demande soumise et prerequis OK
DEFINIR : statut = SOUMIS
DEFINIR : dateDepot = LocalDate.now()
ENVOYER : notification a l'admin
TYPE : ACTION_REQUISE
MESSAGE : "[NomDoctorant] a soumis une demande de soutenance"
IMPLEMENTATION : dans DemandeSoutenanceServiceImpl.ajouter()
```

## REGLE SO-03 : Documents obligatoires soutenance
```
QUAND : une demande de soutenance est soumise
VERIFIER : presence de TOUS ces documents
- DEMANDE_MANUSCRITE (obligatoire)
- RAPPORT_THESE (obligatoire)
- RAPPORT_ANTIPLAGIAT (obligatoire)
- RAPPORT_PUBLICATIONS (obligatoire)
- ATTESTATION_FORMATION minimum 1 (obligatoire)
- AUTORISATION_SOUTENANCE (obligatoire)
SI document manquant -> lancer exception "Document manquant : [typeDocument]"
IMPLEMENTATION : dans DemandeSoutenanceServiceImpl.verifierDocumentsObligatoires()
```

## REGLE SO-04 : Proposition du jury par le directeur
```
QUAND : le directeur propose les membres du jury
VERIFIER : demande.statut == SOUMIS ou PREREQUIS_VALIDES
CHANGER : statut -> EN_ATTENTE_JURY
ENVOYER : notification a l'admin
TYPE : ACTION_REQUISE
MESSAGE : "Le jury pour la soutenance de [NomDoctorant] a ete propose"
IMPLEMENTATION : dans MembreJuryServiceImpl quand jury est complete
```

## REGLE SO-05 : Un seul president du jury
```
QUAND : le directeur ajoute un membre avec role PRESIDENT
VERIFIER : pas deja un PRESIDENT pour cette demande
SI president existe -> lancer exception "Un president du jury existe deja"
IMPLEMENTATION : dans MembreJuryServiceImpl.ajouter()
-> membreJuryRepo.findByDemandeSoutenanceAndRole(demande, PRESIDENT)
```

## REGLE SO-06 : Minimum 2 rapporteurs
```
QUAND : le directeur finalise la composition du jury
VERIFIER : count(membres WHERE role = RAPPORTEUR) >= 2
SI moins de 2 -> lancer exception "Le jury doit comporter au moins 2 rapporteurs"
IMPLEMENTATION : dans MembreJuryServiceImpl.finaliserJury()
```

## REGLE SO-07 : Le directeur ne peut pas etre rapporteur
```
QUAND : le directeur ajoute un membre jury
VERIFIER : le membre n'est pas le directeur lui-meme
SI violation -> lancer exception "Le directeur de these ne peut pas etre rapporteur"
IMPLEMENTATION : dans MembreJuryServiceImpl.ajouter()
```

## REGLE SO-08 : Autorisation admin
```
QUAND : l'admin autorise la soutenance
VERIFIER : demande.statut == EN_ATTENTE_RAPPORTS
CHANGER : soutenance.autorisationAdmin = true
CHANGER : demande.statut -> AUTORISEE
ENVOYER : notification au doctorant TYPE INFO
ENVOYER : notification au directeur TYPE INFO
IMPLEMENTATION : dans SoutenanceServiceImpl.autoriser(id)
```

## REGLE SO-09 : Soutenance deja autorisee
```
QUAND : l'admin tente d'autoriser une soutenance deja autorisee
VERIFIER : soutenance.autorisationAdmin == false
SI deja autorisee -> lancer SoutenanceDejaAutoriseeException(id)
IMPLEMENTATION : dans SoutenanceServiceImpl.autoriser(id)
```

## REGLE SO-10 : Planification de la soutenance
```
QUAND : l'admin planifie la soutenance
VERIFIER : demande.statut == AUTORISEE
VERIFIER : dateSoutenance > LocalDate.now()
VERIFIER : lieu non vide
CHANGER : demande.statut -> PLANIFIEE
ENVOYER : notification au doctorant, directeur et membres jury
TYPE : INFO
MESSAGE : "Soutenance planifiee le [date] a [heure] - [lieu]"
IMPLEMENTATION : dans SoutenanceServiceImpl.planifier(soutenance)
```

## REGLE SO-11 : Date soutenance dans le futur
```
QUAND : l'admin definit la date de soutenance
VERIFIER : dateSoutenance > LocalDate.now()
SI date passee -> lancer exception "La date de soutenance doit etre dans le futur"
IMPLEMENTATION : dans SoutenanceServiceImpl.planifier()
```

---

# MODULE 9 - GESTION DES NOTIFICATIONS

## REGLE NO-01 : Envoi automatique
```
TOUTES LES NOTIFICATIONS sont envoyees automatiquement par les services
JAMAIS : l'utilisateur ne cree manuellement une notification
IMPLEMENTATION : dans chaque ServiceImpl qui declenche un evenement
```

## REGLE NO-02 : Structure d'une notification
```
DEFINIR : message (texte clair et lisible)
DEFINIR : type (INFO / ALERTE / ACTION_REQUISE)
DEFINIR : lienCible (URL vers la ressource concernee)
DEFINIR : destinataire (User concerne)
DEFINIR : dateEnvoi = LocalDateTime.now()
DEFINIR : lu = false
```

## REGLE NO-03 : Marquer comme lue
```
QUAND : l'utilisateur clique sur une notification
CHANGER : lu = true
IMPLEMENTATION : dans NotificationServiceImpl.marquerCommeLue(id)
```

## REGLE NO-04 : Marquer toutes comme lues
```
QUAND : l'utilisateur clique "Tout marquer comme lu"
CHANGER : lu = true pour TOUTES ses notifications
IMPLEMENTATION : dans NotificationServiceImpl.marquerToutesCommeLues(user)
```

## REGLE NO-05 : Badge notifications non lues
```
AFFICHER : dans la navbar le nombre de notifications non lues
CALCULER : notificationService.countNonLues(userConnecte)
IMPLEMENTATION : dans un @ModelAttribute global ou intercepteur Spring
```

---

# MODULE 10 - SECURITE SPRING SECURITY

## REGLE SEC-01 : Protection des URLs par role
```
PUBLIC :
-> /login, /register, /css/**, /js/**, /images/**

CANDIDAT :
-> /dashboard/candidat
-> /dossiers/nouveau (GET et POST)
-> /dossiers (GET)
-> /publications/**
-> /formations/**
-> /soutenances/prerequis
-> /soutenances/demande (GET et POST)
-> /derogations/nouvelle (GET et POST)

DIRECTEUR :
-> /dashboard/directeur
-> /dossiers/validation
-> /dossiers/{id}/valider (POST)
-> /dossiers/{id}/rejeter (POST)
-> /jury/**

ADMIN :
-> /dashboard/admin
-> /campagnes/**
-> /soutenances (GET)
-> /soutenances/{id}/autoriser (POST)
-> /soutenances/{id}/planifier (GET et POST)
-> /derogations (GET)
-> /derogations/{id}/accorder (POST)
-> /derogations/{id}/refuser (POST)

TOUS CONNECTES :
-> /notifications/**
-> /dossiers/{id} (GET)
-> /soutenances/{id} (GET)

IMPLEMENTATION : dans SecurityConfig.filterChain()
```

## REGLE SEC-02 : Controle acces aux donnees
```
QUAND : un candidat accede a ses dossiers
VERIFIER : dossier.doctorant.email == principal.getName()
SI non -> lancer AccessDeniedException ou retourner 403
IMPLEMENTATION : dans chaque methode controller
```

## REGLE SEC-03 : Rate Limiting sur le login
```
LIMITER : 5 tentatives par IP par minute
APRES 5 echecs : bloquer pendant 15 minutes
LOGGER : chaque tentative de connexion
IMPLEMENTATION : via RateLimitFilter personnalise
```

## REGLE SEC-04 : Headers de securite HTTP
```
CONFIGURER dans SecurityConfig :
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000
Referrer-Policy: strict-origin-when-cross-origin
```

## REGLE SEC-05 : CSRF
```
ACTIVER CSRF pour : formulaires Thymeleaf
DESACTIVER CSRF pour : endpoints REST /api/**
IMPLEMENTATION : csrf.ignoringRequestMatchers("/api/**")
```

## REGLE SEC-06 : JWT Configuration
```
GENERER : token JWT a la connexion via /api/auth/login
SECRET : minimum 32 caracteres dans .env
EXPIRATION access token : 1 heure (3600000 ms)
EXPIRATION refresh token : 24 heures (86400000 ms)
ALGORITHM : HS256
VALIDER : a chaque requete via JwtAuthFilter
CONTENU token : { email, role, iat, exp }
IMPLEMENTATION : dans JwtUtils.generateToken() et validateToken()
```

---

# MODULE 11 - TACHES PLANIFIEES

## REGLE TASK-01 : Verification quotidienne des campagnes
```
CRON : "0 0 0 * * *" (chaque nuit a minuit)
FAIRE : trouver campagnes OUVERTE dont dateFermeture < LocalDate.now()
FAIRE : passer statut a FERMEE
IMPLEMENTATION :
@Scheduled(cron = "0 0 0 * * *")
public void fermerCampagnesExpirees() dans ScheduledTaskService
```

## REGLE TASK-02 : Verification hebdomadaire des depassements
```
CRON : "0 0 8 * * MON" (chaque lundi a 8h)
FAIRE : trouver doctorants dont dateInscriptionInitiale < now().minusYears(5)
FAIRE : envoyer alerte approche 6 ans
FAIRE : trouver doctorants > 6 ans
FAIRE : passer statut a SUSPENDU + alerter admin
IMPLEMENTATION :
@Scheduled(cron = "0 0 8 * * MON")
public void verifierDepassements() dans ScheduledTaskService
```

---

# MODULE 12 - UPLOAD DE FICHIERS

## REGLE UP-01 : Validation fichier uploade
```
ETAPE 1 : verifier extension dans [pdf, jpg, jpeg, png]
ETAPE 2 : verifier type MIME reel
ETAPE 3 : verifier taille <= 10MB
ETAPE 4 : renommer avec UUID.randomUUID() + "." + extension
ETAPE 5 : stocker dans /uploads/{typeDocument}/
ETAPE 6 : sauvegarder le chemin en base
SI validation echoue -> lancer DocumentFormatInvalideException(format)
IMPLEMENTATION : dans DocumentServiceImpl.ajouter(MultipartFile file)
```

## REGLE UP-02 : Configuration upload
```
Dans application.properties :
app.upload.dir=/uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
Creer le repertoire si inexistant au demarrage
IMPLEMENTATION : dans FileStorageService
```

---

# MODULE 13 - DASHBOARD

## REGLE DASH-01 : Dashboard Candidat
```
AFFICHER :
- Statut dernier dossier avec badge colore
- Progression prerequis : journauxQ1Q2/2, conferences/2, heures/200
- 5 dernieres notifications non lues
- Alerte si anneeEnCours > 3
- Alerte rouge si anneeEnCours >= 6
- Boutons acces rapide : dossier, publications, soutenance
IMPLEMENTATION : dans DashboardWebController.dashboardCandidat()
```

## REGLE DASH-02 : Dashboard Directeur
```
AFFICHER :
- Nombre dossiers EN_ATTENTE_DIRECTEUR
- Liste dossiers a valider avec boutons valider/rejeter
- Liste doctorants encadres avec statut
- Alerte si doctorant encadre approche 6 ans
IMPLEMENTATION : dans DashboardWebController.dashboardDirecteur()
```

## REGLE DASH-03 : Dashboard Admin
```
AFFICHER :
- Total doctorants ACTIFS
- Nombre dossiers EN_ATTENTE_ADMIN
- Nombre soutenances AUTORISEE ou PLANIFIEE
- Nombre derogations EN_ATTENTE
- Liste doctorants en depassement 6 ans (rouge)
- Campagnes OUVERTE
IMPLEMENTATION : dans DashboardWebController.dashboardAdmin()
```

---

# RESUME TOUTES LES VALIDATIONS

## Dans les DTOs (@Valid)
```
UserDTO :
- nom : @NotBlank
- prenom : @NotBlank
- email : @NotBlank + @Email
- motDePasse : @NotBlank + @Size(min=6)

PublicationDTO :
- titre : @NotBlank
- type : @NotNull
- annee : @NotNull + @Min(1900) + @Max(2030)

FormationDoctoraleDTO :
- intitule : @NotBlank
- heures : @NotNull + @Min(1)
- dateFormation : @NotNull

SoutenanceDTO :
- dateSoutenance : @NotNull
- heure : @NotNull
- lieu : @NotBlank

MembreJuryDTO :
- nom : @NotBlank
- prenom : @NotBlank
- grade : @NotBlank
- etablissement : @NotBlank
- role : @NotNull

DerogationDTO :
- motif : @NotBlank + @Size(min=50)
```

## Dans les Services
```
UserServiceImpl :
- Email unique (existsByEmail)
- Hash BCrypt du mot de passe

DossierInscriptionServiceImpl :
- Campagne ouverte
- Pas depassement 3 ans (ou derogation accordee)
- Pas depassement 6 ans
- Pas de dossier actif en cours
- Documents obligatoires presents
- Commentaire obligatoire si rejet

PublicationServiceImpl :
- Publication appartient au doctorant connecte

FormationDoctoraleServiceImpl :
- Attestation fournie
- Heures > 0

DemandeSoutenanceServiceImpl :
- Tous prerequis remplis (2Q1Q2 + 2conf + 200h)
- Dossier inscription VALIDE
- Doctorant ACTIF et < 6 ans
- Documents soutenance obligatoires

MembreJuryServiceImpl :
- 1 seul president
- Directeur pas rapporteur
- Min 2 rapporteurs

SoutenanceServiceImpl :
- Pas deja autorisee
- Date dans le futur
- Lieu non vide

DerogationServiceImpl :
- Entre 3 et 6 ans
- Pas de derogation EN_ATTENTE existante
```

## Dans les Controllers
```
Tous les Controllers Web :
- BindingResult.hasErrors() -> retourner formulaire avec erreurs
- try/catch sur exceptions metier -> ajouter message au Model
- Toujours redirect apres POST (pattern PRG)
- Recuperer user connecte via Principal principal
- Verifier appartenance des donnees au user connecte
```

---

# ORDRE D'IMPLEMENTATION RECOMMANDE

```
1. UserDetailsServiceImpl + SecurityConfig basique
2. CustomAuthenticationSuccessHandler (redirection par role)
3. JwtUtils + JwtAuthFilter (pour REST)
4. ScheduledTaskService (taches planifiees)
5. FileStorageService (upload fichiers)
6. Completer les regles metier manquantes dans les services
7. Ajouter @PreAuthorize sur les methodes sensibles
8. Tests unitaires (JUnit + Mockito)
9. Controllers REST API
10. Docker + CI/CD
```
