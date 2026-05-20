package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.dto.dossier.DossierFormDTO;
import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.services.interfaces.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller Web pour la gestion des dossiers d'inscription.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /dossiers            → Mes dossiers (Candidat) ou Tous (Admin)</li>
 *   <li>GET /dossiers/nouveau    → Formulaire de soumission</li>
 *   <li>POST /dossiers/nouveau   → Enregistrement du dossier</li>
 *   <li>GET /dossiers/{id}       → Détail d'un dossier</li>
 *   <li>GET /dossiers/validation → Liste des dossiers à valider (Admin/Directeur)</li>
 *   <li>POST /dossiers/{id}/valider</li>
 *   <li>POST /dossiers/{id}/rejeter</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/dossiers")
public class DossierWebController {

    private final IDossierInscriptionService dossierService;
    private final IDirecteurTheseService directeurService;
    private final IDoctorantService doctorantService;
    private final IUserService userService;
    private final ICampagneInscriptionService campagneService;
    private final IDocumentService documentService;

    public DossierWebController(IDossierInscriptionService dossierService,
                                IDirecteurTheseService directeurService,
                                IDoctorantService doctorantService,
                                IUserService userService,
                                ICampagneInscriptionService campagneService,
                                IDocumentService documentService) {
        this.dossierService = dossierService;
        this.directeurService = directeurService;
        this.doctorantService = doctorantService;
        this.userService = userService;
        this.campagneService = campagneService;
        this.documentService = documentService;
    }

    // ══════════════════════════════════════════════
    //  DTOs de projection pour les vues
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DossierView {
        private Long id;
        private String sujetThese;
        private String candidatNom;
        private String directeurNom;
        private LocalDate dateDepot;
        private String statut;
        private String campagne;
        private String commentaire;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentView {
        private Long id;
        private String typeDocument;
        private String nomFichier;
        private LocalDate dateDepot;
    }

    // ══════════════════════════════════════════════
    //  GET /dossiers
    // ══════════════════════════════════════════════

    @GetMapping
    public String listeDossiers(Model model, Principal principal, @RequestParam(required = false) String statut) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", user);

        List<DossierInscription> entities;

        if (user.getRole() == User.Role.CANDIDAT) {
            // Un candidat ne voit que ses propres dossiers
            Doctorant doctorant = (Doctorant) user;
            entities = dossierService.findByDoctorant(doctorant);
        } else if (user.getRole() == User.Role.DIRECTEUR) {
            // Un directeur ne voit que les dossiers de ses doctorants
            DirecteurThese directeur = (DirecteurThese) user;
            entities = dossierService.findByDirecteur(directeur);
        } else {
            // Un admin voit tout
            entities = dossierService.findAll();
        }

        // Appliquer le filtre de statut si présent
        if (statut != null && !statut.isEmpty()) {
            entities = entities.stream()
                    .filter(d -> d.getStatut().name().equals(statut))
                    .collect(Collectors.toList());
        }

        List<DossierView> dossiers = entities.stream()
                .map(this::toView)
                .collect(Collectors.toList());

        model.addAttribute("dossiers", dossiers);
        return "dossiers/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /dossiers/nouveau
    // ══════════════════════════════════════════════

    @GetMapping("/nouveau")
    public String afficherFormulaire(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (user.getRole() != User.Role.CANDIDAT) {
            return "redirect:/dossiers";
        }

        model.addAttribute("dossier", new DossierFormDTO());
        model.addAttribute("directeurs", directeurService.findAll());
        return "dossiers/formulaire";
    }

    // ══════════════════════════════════════════════
    //  POST /dossiers/nouveau
    // ══════════════════════════════════════════════

    @PostMapping("/nouveau")
    public String soumettreDossier(@Valid @ModelAttribute("dossier") DossierFormDTO form,
                                   BindingResult result,
                                   Principal principal,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            model.addAttribute("directeurs", directeurService.findAll());
            return "dossiers/formulaire";
        }

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;

        // Trouver la campagne active (simplification : on prend la première OUVERTE)
        List<CampagneInscription> campagnes = campagneService.findByStatut(CampagneInscription.StatutCampagne.OUVERTE);
        if (campagnes.isEmpty()) {
            result.reject("campagne.close", "Aucune campagne d'inscription n'est actuellement ouverte.");
            model.addAttribute("directeurs", directeurService.findAll());
            return "dossiers/formulaire";
        }

        // Créer l'entité
        DossierInscription entity = new DossierInscription();
        entity.setSujetThese(form.getSujetThese());
        entity.setDateDepot(LocalDate.now());
        entity.setStatut(DossierInscription.StatutDossier.SOUMIS);
        entity.setDoctorant(doctorant);
        entity.setDirecteurThese(directeurService.findById(form.getDirecteurId()));
        entity.setCampagne(campagnes.get(0));

        DossierInscription saved = dossierService.ajouter(entity);

        // Sauvegarder les documents (logique simulée pour les fichiers)
        saveFile(form.getDiplome(), Document.TypeDocument.DIPLOME, saved);
        saveFile(form.getCv(), Document.TypeDocument.CV, saved);
        saveFile(form.getLettreMotivation(), Document.TypeDocument.LETTRE_MOTIVATION, saved);

        if (form.getAutresDocuments() != null) {
            for (MultipartFile file : form.getAutresDocuments()) {
                if (!file.isEmpty()) {
                    saveFile(file, Document.TypeDocument.DEMANDE_MANUSCRITE, saved);
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Votre dossier a été soumis avec succès.");
        return "redirect:/dossiers";
    }

    // ══════════════════════════════════════════════
    //  GET /dossiers/{id}
    // ══════════════════════════════════════════════

    @GetMapping("/{id}")
    public String detailDossier(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        DossierInscription entity = dossierService.findById(id);
        model.addAttribute("dossier", toView(entity));

        List<DocumentView> docViews = documentService.findByDossierInscription(entity).stream()
                .map(doc -> new DocumentView(doc.getId(), doc.getTypeDocument().name(), doc.getNomFichier(), doc.getDateDepot().toLocalDate()))
                .collect(Collectors.toList());
        model.addAttribute("documents", docViews);

        User connectedUser = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", connectedUser);

        return "dossiers/detail";
    }

    // ══════════════════════════════════════════════
    //  GET /dossiers/validation
    // ══════════════════════════════════════════════

    @GetMapping("/validation")
    public String listeValidation(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        List<DossierInscription> entities;

        if (user.getRole() == User.Role.ADMIN) {
            entities = dossierService.findByStatut(DossierInscription.StatutDossier.EN_ATTENTE_ADMIN);
        } else if (user.getRole() == User.Role.DIRECTEUR) {
            DirecteurThese dir = (DirecteurThese) user;
            entities = dossierService.findByDirecteur(dir).stream()
                    .filter(d -> d.getStatut() == DossierInscription.StatutDossier.EN_ATTENTE_DIRECTEUR)
                    .collect(Collectors.toList());
        } else {
            return "redirect:/dashboard";
        }

        model.addAttribute("dossiers", entities.stream().map(this::toView).collect(Collectors.toList()));
        return "dossiers/validation";
    }

    // ══════════════════════════════════════════════
    //  POST Valider / Rejeter
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/valider")
    public String validerDossier(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        DossierInscription dossier = dossierService.findById(id);

        if (user.getRole() == User.Role.DIRECTEUR) {
            dossier.setStatut(DossierInscription.StatutDossier.EN_ATTENTE_ADMIN);
        } else if (user.getRole() == User.Role.ADMIN) {
            dossier.setStatut(DossierInscription.StatutDossier.VALIDE);
        }

        dossierService.modifier(id, dossier);
        return "redirect:/dossiers/validation?success=valide";
    }

    @PostMapping("/{id}/rejeter")
    public String rejeterDossier(@PathVariable Long id, @RequestParam String commentaire) {
        DossierInscription dossier = dossierService.findById(id);
        dossier.setStatut(DossierInscription.StatutDossier.REJETE);
        dossier.setCommentaire(commentaire);
        dossierService.modifier(id, dossier);
        return "redirect:/dossiers/validation?success=rejete";
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private DossierView toView(DossierInscription d) {
        return new DossierView(
                d.getId(),
                d.getSujetThese(),
                d.getDoctorant().getNom() + " " + d.getDoctorant().getPrenom(),
                d.getDirecteurThese() != null ? d.getDirecteurThese().getNom() + " " + d.getDirecteurThese().getPrenom() : "—",
                d.getDateDepot(),
                d.getStatut().name(),
                d.getCampagne() != null ? d.getCampagne().getType() + " " + d.getCampagne().getAnneeUniversitaire() : "—",
                d.getCommentaire()
        );
    }

    private void saveFile(MultipartFile file, Document.TypeDocument type, DossierInscription dossier) {
        if (file == null || file.isEmpty()) return;

        String uploadDir = "uploads/dossiers/" + dossier.getId() + "/";
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
        doc.setFormat(file.getContentType());
        doc.setDossierInscription(dossier);
        documentService.ajouter(doc);
    }
}
