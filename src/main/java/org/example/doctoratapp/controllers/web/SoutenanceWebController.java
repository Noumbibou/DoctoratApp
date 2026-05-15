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
import java.util.List;
import java.util.stream.Collectors;

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

    public SoutenanceWebController(IDemandeSoutenanceService demandeService,
                                   ISoutenanceService soutenanceService,
                                   IDoctorantService doctorantService,
                                   IPublicationService publicationService,
                                   IFormationDoctoraleService formationService,
                                   IUserService userService,
                                   IDocumentService documentService) {
        this.demandeService = demandeService;
        this.soutenanceService = soutenanceService;
        this.doctorantService = doctorantService;
        this.publicationService = publicationService;
        this.formationService = formationService;
        this.userService = userService;
        this.documentService = documentService;
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
        private String lieu;
        private String statut;
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
        long q1 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q1);
        long q2 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q2);
        long conf = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.CONFERENCE);

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

        // 4. Global
        boolean tousOK = (q1 + q2 >= 2) && (conf >= 2) && (heures >= 200) && antiPlagiat;
        model.addAttribute("tousPrerequisOK", tousOK);

        int valides = 0;
        if ((q1 + q2) >= 2) valides++;
        if (conf >= 2) valides++;
        if (heures >= 200) valides++;
        if (antiPlagiat) valides++;
        model.addAttribute("prerequisValides", valides);

        model.addAttribute("connectedUser", user);
        return "soutenances/prerequis";
    }

    // ══════════════════════════════════════════════
    //  GET /soutenances/demande
    // ══════════════════════════════════════════════

    @GetMapping("/demande")
    public String afficherFormulaireDemande(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;

        // Double vérification des prérequis
        if (!demandeService.verifierTousPrerequis(doctorant)) {
            return "redirect:/soutenances/prerequis";
        }

        model.addAttribute("demande", new DemandeSoutenanceFormDTO());
        
        // Données pour le résumé
        long q1 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q1);
        long q2 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q2);
        model.addAttribute("journauxCount", q1 + q2);
        model.addAttribute("conferencesCount", publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.CONFERENCE));
        model.addAttribute("formationsHeures", formationService.getTotalHeures(doctorant));

        return "soutenances/demande";
    }

    @PostMapping("/demande")
    public String soumettreDemande(@ModelAttribute("demande") DemandeSoutenanceFormDTO form,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;

        // Créer la demande
        DemandeSoutenance demande = new DemandeSoutenance();
        demande.setDateDepot(LocalDate.now());
        demande.setStatut(DemandeSoutenance.StatutDemande.SOUMIS);
        demande.setDoctorant(doctorant);
        
        DemandeSoutenance saved = demandeService.ajouter(demande);

        // Sauvegarder les 6 documents obligatoires
        saveSoutenanceFile(form.getDemandeManuscrite(), Document.TypeDocument.DEMANDE_MANUSCRITE, saved);
        saveSoutenanceFile(form.getRapportThese(), Document.TypeDocument.RAPPORT_THESE, saved);
        saveSoutenanceFile(form.getRapportAntiPlagiat(), Document.TypeDocument.RAPPORT_ANTIPLAGIAT, saved);
        saveSoutenanceFile(form.getRapportPublications(), Document.TypeDocument.RAPPORT_PUBLICATIONS, saved);
        saveSoutenanceFile(form.getAttestationsFormations(), Document.TypeDocument.ATTESTATION_FORMATION, saved);
        saveSoutenanceFile(form.getAutorisationSoutenance(), Document.TypeDocument.AUTORISATION_SOUTENANCE, saved);

        redirectAttributes.addFlashAttribute("successMessage", "Votre demande de soutenance a été soumise avec succès.");
        return "redirect:/dashboard/candidat";
    }

    // ══════════════════════════════════════════════
    //  GET /soutenances/liste
    // ══════════════════════════════════════════════

    @GetMapping("/liste")
    public String listeSoutenances(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        List<SoutenanceView> soutenances = soutenanceService.findAll().stream()
                .map(s -> new SoutenanceView(
                        s.getId(),
                        s.getDemandeSoutenance().getDoctorant().getNom() + " " + s.getDemandeSoutenance().getDoctorant().getPrenom(),
                        "Sujet de thèse", // À récupérer via dossier ou demande si ajouté
                        s.getDateSoutenance(),
                        s.getLieu(),
                        s.getDemandeSoutenance().getStatut().name()
                ))
                .collect(Collectors.toList());

        model.addAttribute("soutenances", soutenances);
        return "soutenances/liste";
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private void saveSoutenanceFile(MultipartFile file, Document.TypeDocument type, DemandeSoutenance demande) {
        if (file == null || file.isEmpty()) return;

        Document doc = new Document();
        doc.setTypeDocument(type);
        doc.setNomFichier(file.getOriginalFilename());
        doc.setCheminFichier("uploads/soutenances/" + demande.getId() + "/" + file.getOriginalFilename());
        doc.setFormat("application/pdf");
        doc.setDemandeSoutenance(demande);
        documentService.ajouter(doc);
    }
}
