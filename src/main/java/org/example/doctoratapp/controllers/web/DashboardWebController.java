package org.example.doctoratapp.controllers.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.services.interfaces.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller Web pour les dashboards (admin, candidat et directeur).
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /dashboard/admin     → templates/dashboard/admin.html     (rôle ADMIN)</li>
 *   <li>GET /dashboard/candidat  → templates/dashboard/candidat.html  (rôle CANDIDAT)</li>
 *   <li>GET /dashboard/directeur → templates/dashboard/directeur.html (rôle DIRECTEUR)</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardWebController {

    private final IUserService userService;
    private final IDoctorantService doctorantService;
    private final IDossierInscriptionService dossierService;
    private final ISoutenanceService soutenanceService;
    private final IDerogationService derogationService;
    private final ICampagneInscriptionService campagneService;
    private final IPublicationService publicationService;
    private final IFormationDoctoraleService formationService;
    private final INotificationService notificationService;

    // Injection par constructeur (pas de @Autowired)
    public DashboardWebController(IUserService userService,
                                  IDoctorantService doctorantService,
                                  IDossierInscriptionService dossierService,
                                  ISoutenanceService soutenanceService,
                                  IDerogationService derogationService,
                                  ICampagneInscriptionService campagneService,
                                  IPublicationService publicationService,
                                  IFormationDoctoraleService formationService,
                                  INotificationService notificationService) {
        this.userService = userService;
        this.doctorantService = doctorantService;
        this.dossierService = dossierService;
        this.soutenanceService = soutenanceService;
        this.derogationService = derogationService;
        this.campagneService = campagneService;
        this.publicationService = publicationService;
        this.formationService = formationService;
        this.notificationService = notificationService;
    }

    // ══════════════════════════════════════════════
    //  DTOs internes de projection pour les vues
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorantDepassementView {
        private String nom;
        private String prenom;
        private String email;
        private String sujetThese;
        private String directeurNom;
        private Long anneeInscription;
        private String statut;
        private Long dossierId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampagneView {
        private String nom;
        private LocalDate dateFermeture;
        private int nombreDossiers;
    }

    /**
     * DTO pour les dossiers affichés sur le dashboard directeur.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierDirecteurView {
        private Long id;
        private String candidatNom;
        private String sujetThese;
        private LocalDate dateDepot;
    }

    /**
     * DTO pour les doctorants encadrés affichés sur le dashboard directeur.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorantDirecteurView {
        private String nom;
        private String prenom;
        private String email;
        private String sujetThese;
        private Integer anneeEnCours;
        private String statut;
        private Long anneeInscription;
    }

    // ══════════════════════════════════════════════
    //  GET /dashboard (Redirection selon le rôle)
    // ══════════════════════════════════════════════

    @GetMapping
    public String dashboardRouter(Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User connectedUser = userService.findByEmail(principal.getName());

        switch (connectedUser.getRole()) {
            case ADMIN:
                return "redirect:/dashboard/admin";
            case DIRECTEUR:
                return "redirect:/dashboard/directeur";
            case CANDIDAT:
                return "redirect:/dashboard/candidat";
            default:
                return "redirect:/login";
        }
    }

    // ══════════════════════════════════════════════
    //  GET /dashboard/admin
    // ══════════════════════════════════════════════

    @GetMapping("/admin")
    public String afficherDashboardAdmin(Model model, Principal principal) {
        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        List<Doctorant> doctorantsActifs = doctorantService.findByStatut(Doctorant.Statut.ACTIF);
        model.addAttribute("totalDoctorantsActifs", doctorantsActifs.size());

        List<DossierInscription> dossiersEnAttente = dossierService.findByStatut(
                DossierInscription.StatutDossier.EN_ATTENTE_ADMIN);
        model.addAttribute("dossiersEnAttenteCount", dossiersEnAttente.size());

        List<Soutenance> soutenances = soutenanceService.findAll();
        model.addAttribute("soutenancesPlanifieesCount", soutenances.size());

        List<Derogation> derogationsEnAttente = derogationService.findByStatut(
                Derogation.StatutDerogation.EN_ATTENTE);
        model.addAttribute("derogationsEnAttenteCount", derogationsEnAttente.size());

        List<Doctorant> depassementEntities = doctorantService.findDoctorantsEnDepassement();
        List<DoctorantDepassementView> doctorantsDepassement = depassementEntities.stream()
                .map(this::toDepassementView)
                .collect(Collectors.toList());
        model.addAttribute("doctorantsDepassement", doctorantsDepassement);

        List<CampagneInscription> campagnesOuvertes = campagneService.findByStatut(
                CampagneInscription.StatutCampagne.OUVERTE);
        List<CampagneView> campagnes = campagnesOuvertes.stream()
                .map(this::toCampagneView)
                .collect(Collectors.toList());
        model.addAttribute("campagnes", campagnes);

        return "dashboard/admin";
    }

    // ══════════════════════════════════════════════
    //  GET /dashboard/candidat
    // ══════════════════════════════════════════════

    @GetMapping("/candidat")
    public String afficherDashboardCandidat(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User connectedUser = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", connectedUser);

        if (!(connectedUser instanceof Doctorant)) {
            return "dashboard/candidat";
        }

        Doctorant doctorant = (Doctorant) connectedUser;

        List<DossierInscription> dossiers = dossierService.findByDoctorant(doctorant);
        if (dossiers != null && !dossiers.isEmpty()) {
            DossierInscription dernierDossier = dossiers.stream()
                    .sorted(Comparator.comparing(DossierInscription::getDateDepot).reversed())
                    .findFirst()
                    .orElse(dossiers.get(0));
            model.addAttribute("dossier", dernierDossier);
        }

        long journauxQ1 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q1);
        long journauxQ2 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q2);
        model.addAttribute("journauxCount", journauxQ1 + journauxQ2);

        long conferences = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.CONFERENCE);
        model.addAttribute("conferencesCount", conferences);

        int totalHeures = formationService.getTotalHeures(doctorant);
        model.addAttribute("formationsHeures", totalHeures);

        if (doctorant.getDateInscriptionInitiale() != null) {
            long annees = ChronoUnit.YEARS.between(doctorant.getDateInscriptionInitiale(), LocalDate.now());
            model.addAttribute("anneeInscription", annees);
        }

        List<Notification> allNotifications = notificationService.findByDestinataire(connectedUser);
        if (allNotifications != null && !allNotifications.isEmpty()) {
            List<Notification> recentNotifications = allNotifications.stream()
                    .sorted(Comparator.comparing(Notification::getDateEnvoi).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("recentNotifications", recentNotifications);
        }

        return "dashboard/candidat";
    }

    // ══════════════════════════════════════════════
    //  GET /dashboard/directeur
    // ══════════════════════════════════════════════

    /**
     * Affiche le dashboard du directeur de thèse.
     *
     * <p>Variables envoyées au model :</p>
     * <ul>
     *   <li><strong>connectedUser</strong> : l'utilisateur connecté (Directeur)</li>
     *   <li><strong>dossiersEnAttenteCount</strong> : nombre de dossiers avec statut EN_ATTENTE_DIRECTEUR</li>
     *   <li><strong>doctorantsCount</strong> : nombre total de doctorants uniques encadrés</li>
     *   <li><strong>soutenancesCount</strong> : nombre de soutenances à venir pour ces doctorants</li>
     *   <li><strong>dossiersEnAttente</strong> : liste des dossiers à valider (projection)</li>
     *   <li><strong>doctorants</strong> : liste de tous les doctorants encadrés (projection)</li>
     * </ul>
     */
    @GetMapping("/directeur")
    public String afficherDashboardDirecteur(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", user);

        if (!(user instanceof DirecteurThese)) {
            return "dashboard/directeur";
        }

        DirecteurThese directeur = (DirecteurThese) user;

        // 1. Récupérer tous les dossiers associés à ce directeur
        List<DossierInscription> allDossiers = dossierService.findByDirecteur(directeur);

        // 2. Dossiers en attente de validation par le directeur
        List<DossierDirecteurView> dossiersEnAttente = allDossiers.stream()
                .filter(d -> d.getStatut() == DossierInscription.StatutDossier.EN_ATTENTE_DIRECTEUR)
                .map(d -> new DossierDirecteurView(d.getId(), d.getDoctorant().getNom() + " " + d.getDoctorant().getPrenom(), d.getSujetThese(), d.getDateDepot()))
                .collect(Collectors.toList());
        model.addAttribute("dossiersEnAttente", dossiersEnAttente);
        model.addAttribute("dossiersEnAttenteCount", dossiersEnAttente.size());

        // 3. Liste des doctorants encadrés (uniques)
        Set<Doctorant> doctorantEntities = allDossiers.stream()
                .map(DossierInscription::getDoctorant)
                .collect(Collectors.toSet());

        List<DoctorantDirecteurView> doctorants = doctorantEntities.stream()
                .map(doc -> {
                    // Trouver son dernier dossier pour le sujet
                    DossierInscription lastDossier = allDossiers.stream()
                            .filter(d -> d.getDoctorant().equals(doc))
                            .sorted(Comparator.comparing(DossierInscription::getDateDepot).reversed())
                            .findFirst().orElse(null);

                    long annees = 0;
                    if (doc.getDateInscriptionInitiale() != null) {
                        annees = ChronoUnit.YEARS.between(doc.getDateInscriptionInitiale(), LocalDate.now());
                    }

                    return new DoctorantDirecteurView(
                            doc.getNom(), doc.getPrenom(), doc.getEmail(),
                            lastDossier != null ? lastDossier.getSujetThese() : "—",
                            doc.getAnneeEnCours(),
                            doc.getStatutDoctorant() != null ? doc.getStatutDoctorant().name() : "ACTIF",
                            annees
                    );
                })
                .collect(Collectors.toList());
        model.addAttribute("doctorants", doctorants);
        model.addAttribute("doctorantsCount", doctorants.size());

        // 4. Soutenances à venir
        // On cherche les soutenances liées aux doctorants du directeur
        long soutenancesAvenir = doctorantEntities.stream()
                .flatMap(doc -> doc.getDemandesSoutenance().stream())
                .map(demande -> soutenanceService.findByDemandeSoutenance(demande).orElse(null))
                .filter(s -> s != null && (s.getDateSoutenance() == null || !s.getDateSoutenance().isBefore(LocalDate.now())))
                .count();
        model.addAttribute("soutenancesCount", soutenancesAvenir);

        return "dashboard/directeur";
    }

    // ══════════════════════════════════════════════
    //  Actions Dossier (POST)
    // ══════════════════════════════════════════════

    /**
     * Valide un dossier par le directeur.
     * Le statut passe de EN_ATTENTE_DIRECTEUR à EN_ATTENTE_ADMIN.
     */
    @PostMapping("/dossiers/{id}/valider")
    public String validerDossier(@PathVariable Long id) {
        dossierService.changerStatut(id, DossierInscription.StatutDossier.EN_ATTENTE_ADMIN);
        return "redirect:/dashboard/directeur?success=valide";
    }

    /**
     * Rejette un dossier par le directeur.
     * Le statut passe à REJETE et un commentaire est ajouté.
     */
    @PostMapping("/dossiers/{id}/rejeter")
    public String rejeterDossier(@PathVariable Long id, @RequestParam String commentaire) {
        DossierInscription dossier = dossierService.findById(id);
        dossier.setCommentaire(commentaire);
        dossier.setStatut(DossierInscription.StatutDossier.REJETE);
        dossierService.modifier(id, dossier);
        return "redirect:/dashboard/directeur?success=rejete";
    }

    // ══════════════════════════════════════════════
    //  Méthodes utilitaires de projection
    // ══════════════════════════════════════════════

    private DoctorantDepassementView toDepassementView(Doctorant doc) {
        DoctorantDepassementView view = new DoctorantDepassementView();
        view.setNom(doc.getNom());
        view.setPrenom(doc.getPrenom());
        view.setEmail(doc.getEmail());
        view.setStatut(doc.getStatutDoctorant() != null ? doc.getStatutDoctorant().name() : "ACTIF");

        if (doc.getDateInscriptionInitiale() != null) {
            long annees = ChronoUnit.YEARS.between(doc.getDateInscriptionInitiale(), LocalDate.now());
            view.setAnneeInscription(annees);
        }

        List<DossierInscription> dossiers = doc.getDossiers();
        if (dossiers != null && !dossiers.isEmpty()) {
            DossierInscription dernierDossier = dossiers.stream()
                    .sorted(Comparator.comparing(DossierInscription::getDateDepot).reversed())
                    .findFirst()
                    .orElse(dossiers.get(0));

            view.setSujetThese(dernierDossier.getSujetThese());
            view.setDossierId(dernierDossier.getId());

            if (dernierDossier.getDirecteurThese() != null) {
                DirecteurThese dir = dernierDossier.getDirecteurThese();
                view.setDirecteurNom(dir.getPrenom() + " " + dir.getNom());
            }
        }
        return view;
    }

    private CampagneView toCampagneView(CampagneInscription campagne) {
        CampagneView view = new CampagneView();
        view.setNom(campagne.getType().name() + " " + campagne.getAnneeUniversitaire());
        view.setDateFermeture(campagne.getDateFermeture());
        view.setNombreDossiers(campagne.getDossiers() != null ? campagne.getDossiers().size() : 0);
        return view;
    }
}
