package org.example.doctoratapp.runner;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.FormationDoctorale;
import org.example.doctoratapp.entities.Publication;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.example.doctoratapp.services.interfaces.IDirecteurTheseService;
import org.example.doctoratapp.services.interfaces.IDossierInscriptionService;
import org.example.doctoratapp.services.interfaces.IDoctorantService;
import org.example.doctoratapp.services.interfaces.IFormationDoctoraleService;
import org.example.doctoratapp.services.interfaces.IPublicationService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Profile("dev")
public class TestDataRunner implements CommandLineRunner {

    private final IUserService userService;
    private final IDoctorantService doctorantService;
    private final IDossierInscriptionService dossierService;
    private final IFormationDoctoraleService formationService;
    private final IPublicationService publicationService;
    private final ICampagneInscriptionService campagneService;
    private final IDirecteurTheseService directeurService;

    // Spring injecte automatiquement via le constructeur
    public TestDataRunner(IUserService userService,
                          IDoctorantService doctorantService,
                          IDossierInscriptionService dossierService,
                          IFormationDoctoraleService formationService,
                          IPublicationService publicationService,
                          ICampagneInscriptionService campagneService,
                          IDirecteurTheseService directeurService) {
        this.userService = userService;
        this.doctorantService = doctorantService;
        this.dossierService = dossierService;
        this.formationService = formationService;
        this.publicationService = publicationService;
        this.campagneService = campagneService;
        this.directeurService = directeurService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("        DEBUT DES TESTS                ");
        System.out.println("========================================");

        testUser();
        testDoctorant();
        testPrerequis();

        System.out.println("========================================");
        System.out.println("        FIN DES TESTS                  ");
        System.out.println("========================================");
    }

    private void testUser() {
        System.out.println("\n── TEST USER ──");

        User user = new User();
        user.setNom("Dupont");
        user.setPrenom("Jean");
        user.setEmail("jean.dupont@mail.com");
        user.setMotDePasse("password123");
        user.setRole(User.Role.CANDIDAT);

        User saved = userService.ajouter(user);
        System.out.println("✅ User créé avec id : " + saved.getId());
        System.out.println("✅ Mot de passe hashé : " + saved.getMotDePasse());

        User found = userService.findByEmail("jean.dupont@mail.com");
        System.out.println("✅ User trouvé par email : " + found.getNom());

        found.setNom("NouveauNom");
        User modifie = userService.modifier(found.getId(), found);
        System.out.println("✅ User modifié : " + modifie.getNom());

        userService.supprimer(modifie.getId());
        System.out.println("✅ User supprimé avec id : " + modifie.getId());
    }

    private void testDoctorant() {
        System.out.println("\n── TEST DOCTORANT ──");

        // Nettoyage de l'ancien doctorant de test si existant
        try {
            User existing = userService.findByEmail("alice.martin@mail.com");
            if (existing != null && existing.getRole() == User.Role.CANDIDAT) {
                Doctorant doc = doctorantService.findById(existing.getId());
                publicationService.findByDoctorant(doc).forEach(p -> publicationService.supprimer(p.getId()));
                formationService.findByDoctorant(doc).forEach(f -> formationService.supprimer(f.getId()));
                userService.supprimer(doc.getId());
                System.out.println("🗑️ Ancien doctorant de test Alice Martin nettoyé.");
            }
        } catch (Exception e) {
            // N'existe pas
        }

        Doctorant doctorant = new Doctorant();
        doctorant.setNom("Martin");
        doctorant.setPrenom("Alice");
        doctorant.setEmail("alice.martin@mail.com");
        doctorant.setMotDePasse("password123");
        doctorant.setRole(User.Role.CANDIDAT);
        doctorant.setNumInscription("DOC-2022-001");
        doctorant.setDateInscriptionInitiale(LocalDate.of(2022, 1, 15));
        doctorant.setAnneeEnCours(3);
        doctorant.setStatutDoctorant(Doctorant.Statut.ACTIF);

        Doctorant saved = doctorantService.ajouter(doctorant);
        System.out.println("✅ Doctorant créé avec id : " + saved.getId());

        Doctorant found = doctorantService.findById(saved.getId());
        System.out.println("✅ Doctorant trouvé : " + found.getNom());

        List<Doctorant> depassement = doctorantService.findDoctorantsEnDepassement();
        System.out.println("✅ Doctorants en dépassement (>6 ans) : " + depassement.size());
    }

    private void testPrerequis() {
        System.out.println("\n── TEST PREREQUIS SOUTENANCE ──");

        Doctorant doctorant = doctorantService
                .findByNumInscription("DOC-2022-001")
                .orElseThrow(() -> new RuntimeException("Doctorant non trouvé"));

        boolean pubsOk = publicationService.prerequisPublicationsRemplis(doctorant);
        System.out.println("✅ Prérequis publications (sans pubs) : " + pubsOk);

        // Créer un dossier validé afin que la règle métier des publications soit respectée
        DossierInscription dossier = createOrFindValidatedDossier(doctorant);
        System.out.println("✅ Dossier validé créé pour le doctorant de test : " + dossier.getId());

        for (int i = 0; i < 2; i++) {
            Publication p = new Publication();
            p.setTitre("Journal Q1 - " + i);
            p.setType(Publication.TypePublication.JOURNAL_Q1);
            p.setStatut(Publication.StatutPublication.PUBLIE);
            p.setDoctorant(doctorant);
            publicationService.ajouter(p);
        }

        for (int i = 0; i < 2; i++) {
            Publication p = new Publication();
            p.setTitre("Conference - " + i);
            p.setType(Publication.TypePublication.CONFERENCE);
            p.setStatut(Publication.StatutPublication.PUBLIE);
            p.setDoctorant(doctorant);
            publicationService.ajouter(p);
        }

        boolean pubsOkApres = publicationService.prerequisPublicationsRemplis(doctorant);
        System.out.println("✅ Prérequis publications (avec pubs) : " + pubsOkApres);

        FormationDoctorale formation = new FormationDoctorale();
        formation.setIntitule("Formation Recherche");
        formation.setHeures(200);
        formation.setDateFormation(LocalDate.now());
        formation.setDoctorant(doctorant);
        formationService.ajouter(formation);

        boolean formationOk = formationService.prerequisFormationRemplis(doctorant);
        System.out.println("✅ Prérequis formation (200h) : " + formationOk);
    }

    private DossierInscription createOrFindValidatedDossier(Doctorant doctorant) {
        Optional<DossierInscription> valide = dossierService.findByDoctorant(doctorant).stream()
                .filter(d -> d.getStatut() == DossierInscription.StatutDossier.VALIDE)
                .findFirst();
        if (valide.isPresent()) {
            return valide.get();
        }

        DirecteurThese directeur = ensureDirecteurTest();
        CampagneInscription campagne = ensureCampagneTest();

        DossierInscription dossier = new DossierInscription();
        dossier.setSujetThese("Sujet de thèse de test");
        dossier.setDateDepot(LocalDate.now());
        dossier.setStatut(DossierInscription.StatutDossier.SOUMIS);
        dossier.setDoctorant(doctorant);
        dossier.setDirecteurThese(directeur);
        dossier.setCampagne(campagne);

        DossierInscription saved = dossierService.ajouter(dossier);
        dossierService.changerStatut(saved.getId(), DossierInscription.StatutDossier.VALIDE);
        return dossierService.findById(saved.getId());
    }

    private DirecteurThese ensureDirecteurTest() {
        String email = "directeur.test@mail.com";
        try {
            User existing = userService.findByEmail(email);
            if (existing != null && existing instanceof DirecteurThese) {
                return (DirecteurThese) existing;
            }
        } catch (Exception ignored) {
        }

        DirecteurThese directeur = new DirecteurThese();
        directeur.setNom("Test");
        directeur.setPrenom("Directeur");
        directeur.setEmail(email);
        directeur.setMotDePasse("password123");
        directeur.setRole(User.Role.DIRECTEUR);
        directeur.setGrade("Professeur");
        directeur.setLaboratoire("Laboratoire Test");
        directeur.setSpecialite("Informatique");
        return directeurService.ajouter(directeur);
    }

    private CampagneInscription ensureCampagneTest() {
        String annee = "2025-2026";
        Optional<CampagneInscription> existing = campagneService.findCampagneActive(annee);
        if (existing.isPresent()) {
            return existing.get();
        }

        CampagneInscription campagne = new CampagneInscription();
        campagne.setDateOuverture(LocalDate.now().minusDays(1));
        campagne.setDateFermeture(LocalDate.now().plusMonths(6));
        campagne.setAnneeUniversitaire(annee);
        campagne.setType(CampagneInscription.TypeCampagne.INSCRIPTION);
        campagne.setStatut(CampagneInscription.StatutCampagne.OUVERTE);
        return campagneService.ajouter(campagne);
    }
}
