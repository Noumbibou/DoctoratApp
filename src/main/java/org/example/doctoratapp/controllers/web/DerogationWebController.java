package org.example.doctoratapp.controllers.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.dto.derogation.DerogationDTO;
import org.example.doctoratapp.entities.Derogation;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.IDerogationService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller Web pour la gestion des demandes de dérogation.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /derogations             → Liste des dérogations (Admin)</li>
 *   <li>GET /derogations/nouvelle     → Formulaire de demande (Candidat)</li>
 *   <li>POST /derogations/nouvelle    → Soumission de la demande</li>
 *   <li>POST /derogations/{id}/accorder → Validation (Admin)</li>
 *   <li>POST /derogations/{id}/refuser  → Rejet (Admin)</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/derogations")
public class DerogationWebController {

    private final IDerogationService derogationService;
    private final IUserService userService;

    public DerogationWebController(IDerogationService derogationService, IUserService userService) {
        this.derogationService = derogationService;
        this.userService = userService;
    }

    // ══════════════════════════════════════════════
    //  DTO de projection pour la liste
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DerogationView {
        private Long id;
        private String doctorantNom;
        private String motif;
        private LocalDate dateDemande;
        private String statut;
    }

    // ══════════════════════════════════════════════
    //  GET /derogations (Liste Admin)
    // ══════════════════════════════════════════════

    @GetMapping
    public String listeDerogations(@RequestParam(required = false) String statut,
                                   Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        List<Derogation> derogations;
        if (statut != null && !statut.isEmpty()) {
            derogations = derogationService.findByStatut(Derogation.StatutDerogation.valueOf(statut));
        } else {
            derogations = derogationService.findAll();
        }

        List<DerogationView> views = derogations.stream()
                .map(d -> new DerogationView(
                        d.getId(),
                        d.getDoctorant().getNom() + " " + d.getDoctorant().getPrenom(),
                        d.getMotif(),
                        d.getDateDemande(),
                        d.getStatut().name()
                ))
                .collect(Collectors.toList());

        model.addAttribute("derogations", views);
        return "derogations/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /derogations/nouvelle (Demande Candidat)
    // ══════════════════════════════════════════════

    @GetMapping("/nouvelle")
    public String afficherFormulaire(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        return "derogations/formulaire";
    }

    @PostMapping("/nouvelle")
    public String soumettreDemande(@RequestParam String motif, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }

        Derogation derogation = new Derogation();
        derogation.setMotif(motif);
        derogation.setDateDemande(LocalDate.now());
        derogation.setStatut(Derogation.StatutDerogation.EN_ATTENTE);
        derogation.setDoctorant((Doctorant) user);

        derogationService.ajouter(derogation);

        redirectAttributes.addFlashAttribute("successMessage", "Votre demande de dérogation a été soumise.");
        return "redirect:/dashboard/candidat";
    }

    // ══════════════════════════════════════════════
    //  Actions Admin
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/accorder")
    public String accorderDerogation(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User admin = userService.findByEmail(principal.getName());
        derogationService.accorder(id, admin);

        redirectAttributes.addFlashAttribute("successMessage", "La dérogation a été accordée.");
        return "redirect:/derogations";
    }

    @PostMapping("/{id}/refuser")
    public String refuserDerogation(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User admin = userService.findByEmail(principal.getName());
        derogationService.refuser(id, admin);

        redirectAttributes.addFlashAttribute("successMessage", "La dérogation a été refusée.");
        return "redirect:/derogations";
    }
}
