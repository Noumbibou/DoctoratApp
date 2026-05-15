package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.dto.formation.FormationDoctoraleDTO;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Document;
import org.example.doctoratapp.entities.FormationDoctorale;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.IDocumentService;
import org.example.doctoratapp.services.interfaces.IFormationDoctoraleService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller Web pour la gestion des formations doctorales.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /formations             → Liste des formations et progression des heures</li>
 *   <li>GET /formations/nouvelle     → Formulaire d'ajout</li>
 *   <li>POST /formations/nouvelle    → Enregistrement avec attestation PDF</li>
 *   <li>GET /formations/{id}/edit    → Formulaire de modification</li>
 *   <li>POST /formations/{id}/edit   → Enregistrement modification</li>
 *   <li>POST /formations/{id}/supprimer → Suppression</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/formations")
public class FormationWebController {

    private final IFormationDoctoraleService formationService;
    private final IUserService userService;
    private final IDocumentService documentService;

    public FormationWebController(IFormationDoctoraleService formationService,
                                  IUserService userService,
                                  IDocumentService documentService) {
        this.formationService = formationService;
        this.userService = userService;
        this.documentService = documentService;
    }

    // ══════════════════════════════════════════════
    //  DTO de projection pour la liste
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormationView {
        private Long id;
        private String intitule;
        private Integer heures;
        private LocalDate dateFormation;
        private String attestationPath;
    }

    // ══════════════════════════════════════════════
    //  GET /formations
    // ══════════════════════════════════════════════

    @GetMapping
    public String listeFormations(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }

        Doctorant doctorant = (Doctorant) user;

        // 1. Calcul du total des heures (Objectif : 200h)
        int totalHeures = formationService.getTotalHeures(doctorant);
        model.addAttribute("totalHeures", totalHeures);

        // 2. Liste des formations
        List<FormationView> formations = formationService.findByDoctorant(doctorant).stream()
                .map(this::toView)
                .collect(Collectors.toList());

        model.addAttribute("formations", formations);
        model.addAttribute("connectedUser", user);

        return "formations/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /formations/nouvelle (AJOUT)
    // ══════════════════════════════════════════════

    @GetMapping("/nouvelle")
    public String afficherFormulaireAjout(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("formation", new FormationDoctoraleDTO());
        return "formations/formulaire";
    }

    @PostMapping("/nouvelle")
    public String ajouterFormation(@Valid @ModelAttribute("formation") FormationDoctoraleDTO dto,
                                   BindingResult result,
                                   @RequestParam(value = "attestation", required = false) MultipartFile file,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "formations/formulaire";
        }

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;

        FormationDoctorale formation = new FormationDoctorale();
        formation.setIntitule(dto.getIntitule());
        formation.setHeures(dto.getHeures());
        formation.setDateFormation(dto.getDateFormation());
        formation.setDoctorant(doctorant);

        FormationDoctorale saved = formationService.ajouter(formation);

        // Gestion du document attestation
        if (file != null && !file.isEmpty()) {
            Document doc = new Document();
            doc.setTypeDocument(Document.TypeDocument.ATTESTATION_FORMATION);
            doc.setNomFichier(file.getOriginalFilename());
            doc.setCheminFichier("uploads/formations/" + saved.getId() + "/" + file.getOriginalFilename());
            doc.setFormat("application/pdf");
            doc.setFormation(saved);
            documentService.ajouter(doc);
        }

        redirectAttributes.addFlashAttribute("successMessage", "La formation a été ajoutée.");
        return "redirect:/formations";
    }

    // ══════════════════════════════════════════════
    //  GET /formations/{id}/edit (MODIFICATION)
    // ══════════════════════════════════════════════

    @GetMapping("/{id}/edit")
    public String afficherFormulaireModif(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        FormationDoctorale f = formationService.findById(id);
        if (!f.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/formations";
        }

        FormationDoctoraleDTO dto = new FormationDoctoraleDTO();
        dto.setId(f.getId());
        dto.setIntitule(f.getIntitule());
        dto.setHeures(f.getHeures());
        dto.setDateFormation(f.getDateFormation());

        model.addAttribute("formation", dto);
        return "formations/formulaire";
    }

    @PostMapping("/{id}/edit")
    public String modifierFormation(@PathVariable Long id,
                                    @Valid @ModelAttribute("formation") FormationDoctoraleDTO dto,
                                    BindingResult result,
                                    @RequestParam(value = "attestation", required = false) MultipartFile file,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "formations/formulaire";
        }

        FormationDoctorale existing = formationService.findById(id);
        if (!existing.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/formations";
        }

        existing.setIntitule(dto.getIntitule());
        existing.setHeures(dto.getHeures());
        existing.setDateFormation(dto.getDateFormation());

        formationService.modifier(id, existing);

        // Optionnel : Mettre à jour l'attestation si un nouveau fichier est fourni
        if (file != null && !file.isEmpty()) {
            // Supprimer l'ancienne ? Logique métier à définir. 
            // Ici on ajoute ou remplace simplement.
            Document doc = new Document();
            doc.setTypeDocument(Document.TypeDocument.ATTESTATION_FORMATION);
            doc.setNomFichier(file.getOriginalFilename());
            doc.setCheminFichier("uploads/formations/" + id + "/" + file.getOriginalFilename());
            doc.setFormat("application/pdf");
            doc.setFormation(existing);
            documentService.ajouter(doc);
        }

        redirectAttributes.addFlashAttribute("successMessage", "La formation a été mise à jour.");
        return "redirect:/formations";
    }

    // ══════════════════════════════════════════════
    //  POST /formations/{id}/supprimer (SUPPRESSION)
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/supprimer")
    public String supprimerFormation(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        FormationDoctorale f = formationService.findById(id);
        if (!f.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/formations";
        }

        formationService.supprimer(id);
        redirectAttributes.addFlashAttribute("successMessage", "La formation a été supprimée.");
        return "redirect:/formations";
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private FormationView toView(FormationDoctorale f) {
        // On récupère le chemin de l'attestation si elle existe via le service document
        // Ou on peut enrichir l'entité FormationDoctorale si elle a un lien vers Document
        // Ici on suppose qu'on peut retrouver le document lié.
        return new FormationView(
                f.getId(),
                f.getIntitule(),
                f.getHeures(),
                f.getDateFormation(),
                (f.getAttestation() != null) ? "exists" : null 
        );
    }
}
