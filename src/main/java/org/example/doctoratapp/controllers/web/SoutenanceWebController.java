package org.example.doctoratapp.controllers.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.dto.soutenance.DemandeSoutenanceFormDTO;
import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.services.interfaces.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Controller Web pour la gestion des soutenances.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /soutenances/prerequis → Vérification des conditions de soutenance</li>
 *   <li>GET /soutenances/demande    → Formulaire de dépôt du manuscrit</li>
 *   <li>POST /soutenances/demande   → Enregistrement de la demande</li>
 *   <li>GET /soutenances/liste      → Liste des soutenances planifiées</li>
 *   <li>GET /soutenances/detail/{id} → Détail d'une soutenance</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/soutenances")
public class SoutenanceWebController {

    private final IDemandeSoutenanceService demandeService;
    private final ISoutenanceService soutenanceService;
    private final IDoctorantService doctorantService;
    private final IPublicationService publicationService;
    private final IFormationDoctoraleService formationService;
    private final IUserService userService;
    private final IDocumentService documentService;
    private final IDossierInscriptionService dossierService;
    private final IMembreJuryService juryService;

    public SoutenanceWebController(IDemandeSoutenanceService demandeService,
                                   ISoutenanceService soutenanceService,
                                   IDoctorantService doctorantService,
                                   IPublicationService publicationService,
                                   IFormationDoctoraleService formationService,
                                   IUserService userService,
                                   IDocumentService documentService,
                                   IDossierInscriptionService dossierService,
                                   IMembreJuryService juryService) {
        this.demandeService = demandeService;
        this.soutenanceService = soutenanceService;
        this.doctorantService = doctorantService;
        this.publicationService = publicationService;
        this.formationService = formationService;
        this.userService = userService;
        this.documentService = documentService;
        this.dossierService = dossierService;
        this.juryService = juryService;
    }

    // ══════════════════════════════════════════════
    //  DTOs de projection
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoutenanceView {
        private Long id;
        private String doctorantNom;
        private String sujetThese;
        private LocalDate dateSoutenance;
        private String heureSoutenance;
        private String lieu;
        private String statut;
        private LocalDate dateDepot;
        private String directeurNom;
    }

    // ══════════════════════════════════════════════
    //  GET /soutenances/prerequis
    // ══════════════════════════════════════════════

    @GetMapping("/prerequis")
    public String verifierPrerequis(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }

        Doctorant doctorant = (Doctorant) user;

        // 1. Publications
        List<Publication.StatutPublication> statutsValides = List.of(
                Publication.StatutPublication.ACCEPTE,
                Publication.StatutPublication.PUBLIE
        );

        long q1 = publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q1, statutsValides);
        long q2 = publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q2, statutsValides);
        long conf = publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.CONFERENCE, statutsValides);

        model.addAttribute("journauxCount", q1 + q2);
        model.addAttribute("journauxOK", (q1 + q2) >= 2);
        
        model.addAttribute("conferencesCount", conf);
        model.addAttribute("conferencesOK", conf >= 2);

        // 2. Formations
        int heures = formationService.getTotalHeures(doctorant);
        model.addAttribute("formationsHeures", heures);
        model.addAttribute("formationsOK", heures >= 200);

        // 3. Anti-plagiat (Supposons qu'il doive être présent dans un document du dernier dossier valide)
        // Pour simplifier, on vérifie si un document de type RAPPORT_ANTIPLAGIAT existe pour ce doctorant
        boolean antiPlagiat = documentService.findByType(Document.TypeDocument.RAPPORT_ANTIPLAGIAT).stream()
                .anyMatch(d -> d.getDossierInscription() != null && d.getDossierInscription().getDoctorant().getId().equals(doctorant.getId()));
        model.addAttribute("antiPlagiatOK", antiPlagiat);

        // 4. Global — seuls les prérequis techniques sont requis pour débloquer la demande
        boolean tousOK = (q1 + q2 >= 2) && (conf >= 2) && (heures >= 200);
        model.addAttribute("tousPrerequisOK", tousOK);

        int valides = 0;
        if ((q1 + q2) >= 2) valides++;
        if (conf >= 2) valides++;
        if (heures >= 200) valides++;
        model.addAttribute("prerequisValides", valides);

        model.addAttribute("connectedUser", user);
        return "soutenances/prerequis";
    }

    // ══════════════════════════════════════════════
    //  GET /soutenances/demande
    // ══════════════════════════════════════════════

    @GetMapping("/demande")
    public String afficherFormulaireDemande(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }
        Doctorant doctorant = (Doctorant) user;

        // Double vérification des prérequis
        if (!demandeService.verifierTousPrerequis(doctorant)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous devez d'abord remplir les prérequis publications/formations avant de soumettre une demande.");
            return "redirect:/soutenances/prerequis";
        }

        model.addAttribute("demande", new DemandeSoutenanceFormDTO());
        
        // Données pour le résumé
        List<Publication.StatutPublication> statutsValides = List.of(
                Publication.StatutPublication.ACCEPTE,
                Publication.StatutPublication.PUBLIE
        );
        long q1 = publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q1, statutsValides);
        long q2 = publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q2, statutsValides);
        model.addAttribute("journauxCount", q1 + q2);
        model.addAttribute("conferencesCount", publicationService.countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.CONFERENCE, statutsValides));
        model.addAttribute("formationsHeures", formationService.getTotalHeures(doctorant));

        return "soutenances/demande";
    }

    @PostMapping("/demande")
    public String soumettreDemande(@ModelAttribute("demande") DemandeSoutenanceFormDTO form,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }
        Doctorant doctorant = (Doctorant) user;

        List<String> fileErrors = validateSoutenanceFiles(form);
        if (!fileErrors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", String.join(" ", fileErrors));
            return "redirect:/soutenances/demande";
        }

        try {
            DemandeSoutenance demande = new DemandeSoutenance();
            demande.setDateDepot(LocalDate.now());
            demande.setDoctorant(doctorant);

            DemandeSoutenance saved = demandeService.ajouter(demande);

            saveSoutenanceFile(form.getDemandeManuscrite(), Document.TypeDocument.DEMANDE_MANUSCRITE, saved);
            saveSoutenanceFile(form.getRapportThese(), Document.TypeDocument.RAPPORT_THESE, saved);
            saveSoutenanceFile(form.getRapportAntiPlagiat(), Document.TypeDocument.RAPPORT_ANTIPLAGIAT, saved);
            saveSoutenanceFile(form.getRapportPublications(), Document.TypeDocument.RAPPORT_PUBLICATIONS, saved);
            saveSoutenanceFile(form.getAttestationsFormations(), Document.TypeDocument.ATTESTATION_FORMATION, saved);
            saveSoutenanceFile(form.getAutorisationSoutenance(), Document.TypeDocument.AUTORISATION_SOUTENANCE, saved);

            redirectAttributes.addFlashAttribute("successMessage", "Votre demande de soutenance a été soumise avec succès.");
            return "redirect:/dashboard/candidat";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/soutenances/prerequis";
        }
    }

    // ══════════════════════════════════════════════
    //  GET /soutenances, /soutenances/ et /soutenances/liste
    // ══════════════════════════════════════════════

    @GetMapping({"", "/", "/liste"})
    public String listeSoutenances(@RequestParam(value = "statut", required = false) String statusFilter, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", user);

        List<DemandeSoutenance> demandesList;
        if (user.getRole() == User.Role.ADMIN) {
            demandesList = demandeService.findAll();
        } else if (user.getRole() == User.Role.DIRECTEUR) {
            DirecteurThese directeur = (DirecteurThese) user;
            List<DossierInscription> dossiers = dossierService.findByDirecteur(directeur);
            List<Doctorant> doctorants = dossiers.stream()
                    .map(DossierInscription::getDoctorant)
                    .distinct()
                    .collect(Collectors.toList());
            demandesList = doctorants.stream()
                    .flatMap(doc -> demandeService.findByDoctorant(doc).stream())
                    .collect(Collectors.toList());
        } else {
            Doctorant doc = (Doctorant) user;
            demandesList = demandeService.findByDoctorant(doc);
        }

        if (statusFilter != null && !statusFilter.isEmpty()) {
            demandesList = demandesList.stream()
                    .filter(d -> d.getStatut().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        List<SoutenanceView> views = demandesList.stream()
                .map(d -> {
                    String sujet = "Sujet de thèse";
                    List<DossierInscription> dossiers = dossierService.findByDoctorant(d.getDoctorant());
                    if (dossiers != null && !dossiers.isEmpty()) {
                        sujet = dossiers.stream()
                                .sorted(Comparator.comparing(DossierInscription::getDateDepot).reversed())
                                .findFirst()
                                .map(DossierInscription::getSujetThese)
                                .orElse("Sujet de thèse");
                    }
                    String dirNom = "Non spécifié";
                    if (dossiers != null && !dossiers.isEmpty() && dossiers.get(0).getDirecteurThese() != null) {
                        dirNom = dossiers.get(0).getDirecteurThese().getNom() + " " + dossiers.get(0).getDirecteurThese().getPrenom();
                    }
                    Soutenance s = d.getSoutenance();
                    return new SoutenanceView(
                            d.getId(),
                            d.getDoctorant().getNom() + " " + d.getDoctorant().getPrenom(),
                            sujet,
                            s != null ? s.getDateSoutenance() : null,
                            s != null && s.getHeure() != null ? s.getHeure().toString() : null,
                            s != null ? s.getLieu() : "Non planifiée",
                            d.getStatut().name(),
                            d.getDateDepot(),
                            dirNom
                    );
                })
                .collect(Collectors.toList());

        model.addAttribute("soutenances", views);
        return "soutenances/liste";
    }

    @GetMapping("/{id}")
    public String detailSoutenance(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", user);

        DemandeSoutenance demande = demandeService.findById(id);
        model.addAttribute("soutenance", toView(demande));
        model.addAttribute("documents", documentService.findByDemandeSoutenance(demande));
        model.addAttribute("jury", juryService.findByDemandeSoutenance(demande));

        return "soutenances/detail";
    }

    @PostMapping("/{id}/autoriser")
    public String autoriserSoutenance(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (user.getRole() != User.Role.ADMIN) {
            return "redirect:/soutenances";
        }

        soutenanceService.autoriser(id);
        return "redirect:/soutenances/" + id;
    }

    @GetMapping("/{id}/planifier")
    public String afficherFormulairePlanification(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (user.getRole() != User.Role.ADMIN) {
            return "redirect:/soutenances";
        }

        DemandeSoutenance demande = demandeService.findById(id);
        model.addAttribute("connectedUser", user);
        model.addAttribute("soutenance", toView(demande));

        return "soutenances/planification";
    }

    @PostMapping("/{id}/planifier")
    public String planifierSoutenance(@PathVariable Long id,
                                     @RequestParam("dateSoutenance") LocalDate dateSoutenance,
                                     @RequestParam("heureSoutenance") LocalTime heureSoutenance,
                                     @RequestParam("lieu") String lieu,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (user.getRole() != User.Role.ADMIN) {
            return "redirect:/soutenances";
        }

        DemandeSoutenance demande = demandeService.findById(id);
        Soutenance soutenance = soutenanceService.findByDemandeSoutenance(demande)
                .orElseGet(() -> {
                    Soutenance s = new Soutenance();
                    s.setDemandeSoutenance(demande);
                    s.setAutorisationAdmin(true);
                    return s;
                });

        soutenance.setDateSoutenance(dateSoutenance);
        soutenance.setHeure(heureSoutenance);
        soutenance.setLieu(lieu);
        soutenanceService.planifier(soutenance);
        demandeService.changerStatut(id, DemandeSoutenance.StatutDemande.PLANIFIEE);

        redirectAttributes.addFlashAttribute("successMessage", "Soutenance planifiée avec succès.");
        return "redirect:/soutenances/" + id;
    }

    private SoutenanceView toView(DemandeSoutenance demande) {
        String sujet = "Sujet de thèse";
        String directeurNom = "Non spécifié";
        List<DossierInscription> dossiers = dossierService.findByDoctorant(demande.getDoctorant());
        if (dossiers != null && !dossiers.isEmpty()) {
            DossierInscription dossier = dossiers.stream()
                    .sorted(Comparator.comparing(DossierInscription::getDateDepot).reversed())
                    .findFirst()
                    .orElse(dossiers.get(0));

            if (dossier.getSujetThese() != null) {
                sujet = dossier.getSujetThese();
            }
            if (dossier.getDirecteurThese() != null) {
                directeurNom = dossier.getDirecteurThese().getNom() + " " + dossier.getDirecteurThese().getPrenom();
            }
        }

        Soutenance s = demande.getSoutenance();
        if (s == null) {
            s = soutenanceService.findByDemandeSoutenance(demande).orElse(null);
        }

        return new SoutenanceView(
                demande.getId(),
                demande.getDoctorant().getNom() + " " + demande.getDoctorant().getPrenom(),
                sujet,
                s != null ? s.getDateSoutenance() : null,
                s != null && s.getHeure() != null ? s.getHeure().toString() : null,
                s != null ? s.getLieu() : "Non planifiée",
                demande.getStatut().name(),
                demande.getDateDepot(),
                directeurNom
        );
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private void saveSoutenanceFile(MultipartFile file, Document.TypeDocument type, DemandeSoutenance demande) {
        if (file == null || file.isEmpty()) return;

        String uploadDir = "uploads/soutenances/" + demande.getId() + "/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        java.io.File dest = new java.io.File(dir, file.getOriginalFilename());
        try {
            java.nio.file.Files.copy(file.getInputStream(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        Document doc = new Document();
        doc.setTypeDocument(type);
        doc.setNomFichier(file.getOriginalFilename());
        doc.setCheminFichier(uploadDir + file.getOriginalFilename());
        doc.setFormat("application/pdf");
        doc.setDemandeSoutenance(demande);
        documentService.ajouter(doc);
    }

    private List<String> validateSoutenanceFiles(DemandeSoutenanceFormDTO form) {
        List<String> errors = new ArrayList<>();
        validateFile(form.getDemandeManuscrite(), "Demande manuscrite", errors);
        validateFile(form.getRapportThese(), "Rapport de thèse", errors);
        validateFile(form.getRapportAntiPlagiat(), "Rapport anti-plagiat", errors);
        validateFile(form.getRapportPublications(), "Rapport publications", errors);
        validateFile(form.getAttestationsFormations(), "Attestations formations", errors);
        validateFile(form.getAutorisationSoutenance(), "Autorisation de soutenance", errors);
        return errors;
    }

    private void validateFile(MultipartFile file, String label, List<String> errors) {
        if (file == null || file.isEmpty()) {
            errors.add(label + " est obligatoire.");
            return;
        }
        if (!isPdfFile(file)) {
            errors.add(label + " doit être un fichier PDF.");
        }
        if (isTooLarge(file)) {
            errors.add(label + " doit faire moins de 10 Mo.");
        }
    }

    private boolean isPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return false;
        }
        String filename = file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType();
        return filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType);
    }

    private boolean isTooLarge(MultipartFile file) {
        return file != null && file.getSize() > 10L * 1024 * 1024;
    }
}
