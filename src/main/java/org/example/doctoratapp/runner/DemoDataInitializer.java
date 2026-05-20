package org.example.doctoratapp.runner;

import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.exceptions.UserNotFoundException;
import org.example.doctoratapp.services.interfaces.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Initialiseur de données pour la démonstration.
 * Crée les comptes par défaut et quelques données de test.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final IUserService userService;
    private final IDoctorantService doctorantService;
    private final IDirecteurTheseService directeurService;
    private final IPublicationService publicationService;
    private final IFormationDoctoraleService formationService;
    private final IDossierInscriptionService dossierService;
    private final ICampagneInscriptionService campagneService;

    public DemoDataInitializer(IUserService userService, 
                               IDoctorantService doctorantService, 
                               IDirecteurTheseService directeurService,
                               IPublicationService publicationService,
                               IFormationDoctoraleService formationService,
                               IDossierInscriptionService dossierService,
                               ICampagneInscriptionService campagneService) {
        this.userService = userService;
        this.doctorantService = doctorantService;
        this.directeurService = directeurService;
        this.publicationService = publicationService;
        this.formationService = formationService;
        this.dossierService = dossierService;
        this.campagneService = campagneService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Initialisation des comptes de démonstration...");

        // Nettoyage automatique des candidats ou directeurs incorrectement typés en base (problème d'héritage)
        try {
            for (User u : userService.findAll()) {
                if (u.getRole() == User.Role.CANDIDAT) {
                    try {
                        doctorantService.findById(u.getId());
                    } catch (Exception e) {
                        userService.supprimer(u.getId());
                        System.out.println("🗑️ Suppression du compte candidat invalide pour permettre sa réinscription : " + u.getEmail());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur nettoyage candidats invalides : " + e.getMessage());
        }

        // 0. Créer une campagne d'inscription active
        CampagneInscription campagne = null;
        if (campagneService.findAll().isEmpty()) {
            campagne = new CampagneInscription();
            campagne.setAnneeUniversitaire("2024-2025");
            campagne.setType(CampagneInscription.TypeCampagne.INSCRIPTION);
            campagne.setStatut(CampagneInscription.StatutCampagne.OUVERTE);
            campagne.setDateOuverture(LocalDate.now().minusDays(10));
            campagne.setDateFermeture(LocalDate.now().plusMonths(2));
            campagne = campagneService.ajouter(campagne);
            System.out.println("✅ Campagne 2024-2025 créée.");
        } else {
            campagne = campagneService.findAll().get(0);
        }

        // 1. Créer un Administrateur
        try {
            userService.findByEmail("admin@doctorat.ma");
        } catch (UserNotFoundException e) {
            User admin = new User();
            admin.setNom("Admin");
            admin.setPrenom("Système");
            admin.setEmail("admin@doctorat.ma");
            admin.setMotDePasse("admin123");
            admin.setRole(User.Role.ADMIN);
            userService.ajouter(admin);
            System.out.println("✅ ADMIN : admin@doctorat.ma / admin123");
        }

        // 2. Créer un Directeur de Thèse
        DirecteurThese directeur = null;
        try {
            directeur = (DirecteurThese) userService.findByEmail("directeur@doctorat.ma");
        } catch (UserNotFoundException e) {
            DirecteurThese dir = new DirecteurThese();
            dir.setNom("ALAMI");
            dir.setPrenom("Mohammed");
            dir.setEmail("directeur@doctorat.ma");
            dir.setMotDePasse("directeur123");
            dir.setRole(User.Role.DIRECTEUR);
            dir.setGrade("PES");
            dir.setSpecialite("Informatique");
            dir.setLaboratoire("FS - Rabat");
            directeur = directeurService.ajouter(dir);
            System.out.println("✅ DIRECTEUR : directeur@doctorat.ma / directeur123");
        }

        // 3. Créer un Doctorant
        Doctorant doctorant = null;
        try {
            doctorant = (Doctorant) userService.findByEmail("candidat@doctorat.ma");
        } catch (UserNotFoundException e) {
            Doctorant doc = new Doctorant();
            doc.setNom("BENNANI");
            doc.setPrenom("Yassine");
            doc.setEmail("candidat@doctorat.ma");
            doc.setMotDePasse("candidat123");
            doc.setRole(User.Role.CANDIDAT);
            doc.setNumInscription("DOC-2024-001");
            doc.setDateInscriptionInitiale(LocalDate.of(2024, 9, 1));
            doc.setAnneeEnCours(1);
            doc.setStatutDoctorant(Doctorant.Statut.ACTIF);
            doctorant = doctorantService.ajouter(doc);
            System.out.println("✅ CANDIDAT : candidat@doctorat.ma / candidat123");

            // 4. Créer un dossier d'inscription pour lier Doctorant et Directeur
            DossierInscription dossier = new DossierInscription();
            dossier.setSujetThese("Optimisation des algorithmes de Machine Learning sur Edge Computing");
            dossier.setDoctorant(doctorant);
            dossier.setDirecteurThese(directeur);
            dossier.setCampagne(campagne); // Correction : ajout de la campagne
            // Pour la démo, marquer le dossier comme VALIDE afin de permettre l'ajout de publications
            dossier.setStatut(DossierInscription.StatutDossier.VALIDE);
            dossier.setDateDepot(LocalDate.now());
            DossierInscription saved = dossierService.ajouter(dossier);
            // Pour la démo : valider le dossier afin d'autoriser les publications associées
            dossierService.changerStatut(saved.getId(), DossierInscription.StatutDossier.VALIDE);
            System.out.println("✅ Dossier d'inscription créé, lié et validé pour la démo.");

            // 5. Ajouter quelques données pour garnir le dashboard
            Publication p = new Publication();
            p.setTitre("Deep Learning for IoT Security");
            p.setType(Publication.TypePublication.JOURNAL_Q1);
            p.setStatut(Publication.StatutPublication.SOUMIS);
            p.setDoctorant(doctorant);
            publicationService.ajouter(p);

            FormationDoctorale f = new FormationDoctorale();
            f.setIntitule("Méthodologie de rédaction scientifique");
            f.setHeures(30);
            f.setDoctorant(doctorant);
            f.setDateFormation(LocalDate.now());
            formationService.ajouter(f);
            System.out.println("✅ Données initiales (1 pub, 30h formation) ajoutées.");
        }

        System.out.println("✨ Initialisation terminée. Prêt pour la démo !");
    }
}
