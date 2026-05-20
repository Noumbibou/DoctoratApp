package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.dto.publication.PublicationDTO;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Publication;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.IPublicationService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller Web pour la gestion des publications scientifiques.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /publications             → Liste des publications et compteurs de prérequis</li>
 *   <li>GET /publications/nouvelle     → Formulaire d'ajout</li>
 *   <li>POST /publications/nouvelle    → Enregistrement de l'ajout</li>
 *   <li>GET /publications/{id}/edit    → Formulaire de modification</li>
 *   <li>POST /publications/{id}/edit   → Enregistrement de la modification</li>
 *   <li>POST /publications/{id}/supprimer → Suppression</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/publications")
public class PublicationWebController {

    private final IPublicationService publicationService;
    private final IUserService userService;

    public PublicationWebController(IPublicationService publicationService, IUserService userService) {
        this.publicationService = publicationService;
        this.userService = userService;
    }

    // ══════════════════════════════════════════════
    //  DTO de projection pour la liste
    // ══════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationView {
        private Long id;
        private String titre;
        private String type;
        private String revue;
        private Integer annee;
        private String statut;
        private String url;
    }

    // ══════════════════════════════════════════════
    //  GET /publications
    // ══════════════════════════════════════════════

    @GetMapping
    public String listePublications(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        if (!(user instanceof Doctorant)) {
            return "redirect:/dashboard";
        }

        Doctorant doctorant = (Doctorant) user;

        // 1. Compteurs de prérequis
        long q1 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q1);
        long q2 = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q2);
        long conf = publicationService.countByDoctorantAndType(doctorant, Publication.TypePublication.CONFERENCE);

        model.addAttribute("journauxCount", q1 + q2);
        model.addAttribute("conferencesCount", conf);

        // 2. Liste des publications
        List<PublicationView> publications = publicationService.findByDoctorant(doctorant).stream()
                .map(this::toView)
                .collect(Collectors.toList());

        model.addAttribute("publications", publications);
        model.addAttribute("connectedUser", user);

        return "publications/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /publications/nouvelle (AJOUT)
    // ══════════════════════════════════════════════

    @GetMapping("/nouvelle")
    public String afficherFormulaireAjout(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;
        if (!isDoctorantActif(doctorant)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Votre statut ne permet pas de soumettre des publications.");
            return "redirect:/dashboard";
        }

        model.addAttribute("publication", new PublicationDTO());
        return "publications/formulaire";
    }

    @PostMapping("/nouvelle")
    public String ajouterPublication(@Valid @ModelAttribute("publication") PublicationDTO dto,
                                     BindingResult result,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "publications/formulaire";
        }

        User user = userService.findByEmail(principal.getName());
        Doctorant doctorant = (Doctorant) user;
        if (!isDoctorantActif(doctorant)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Votre statut ne permet pas de soumettre des publications.");
            return "redirect:/publications";
        }

        Publication publication = new Publication();
        updateEntityFromDto(publication, dto);
        publication.setDoctorant(doctorant);
        publication.setStatut(Publication.StatutPublication.SOUMIS);

        publicationService.ajouter(publication);

        redirectAttributes.addFlashAttribute("successMessage", "La publication a été ajoutée avec succès.");
        return "redirect:/publications";
    }

    // ══════════════════════════════════════════════
    //  GET /publications/{id}/edit (MODIFICATION)
    // ══════════════════════════════════════════════

    @GetMapping("/{id}/edit")
    public String afficherFormulaireModif(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Publication pub = publicationService.findById(id);
        
        // Sécurité : vérifier que la publication appartient au doctorant connecté
        if (!pub.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/publications";
        }
        if (!isDoctorantActif((Doctorant) userService.findByEmail(principal.getName()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Votre statut ne permet pas de modifier des publications.");
            return "redirect:/publications";
        }

        PublicationDTO dto = new PublicationDTO();
        dto.setId(pub.getId());
        dto.setTitre(pub.getTitre());
        dto.setType(pub.getType().name());
        dto.setRevue(pub.getRevue());
        dto.setAnnee(pub.getAnnee());
        dto.setUrl(pub.getUrl());
        dto.setStatut(pub.getStatut().name());

        model.addAttribute("publication", dto);
        return "publications/formulaire";
    }

    @PostMapping("/{id}/edit")
    public String modifierPublication(@PathVariable Long id,
                                      @Valid @ModelAttribute("publication") PublicationDTO dto,
                                      BindingResult result,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "publications/formulaire";
        }

        Publication existing = publicationService.findById(id);
        if (!existing.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/publications";
        }
        Doctorant doctorant = (Doctorant) userService.findByEmail(principal.getName());
        if (!isDoctorantActif(doctorant)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Votre statut ne permet pas de modifier des publications.");
            return "redirect:/publications";
        }

        updateEntityFromDto(existing, dto);
        publicationService.modifier(id, existing);

        redirectAttributes.addFlashAttribute("successMessage", "La publication a été mise à jour.");
        return "redirect:/publications";
    }

    // ══════════════════════════════════════════════
    //  POST /publications/{id}/supprimer (SUPPRESSION)
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/supprimer")
    public String supprimerPublication(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Publication existing = publicationService.findById(id);
        if (!existing.getDoctorant().getEmail().equals(principal.getName())) {
            return "redirect:/publications";
        }

        publicationService.supprimer(id);
        redirectAttributes.addFlashAttribute("successMessage", "La publication a été supprimée.");
        return "redirect:/publications";
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private PublicationView toView(Publication p) {
        return new PublicationView(
                p.getId(),
                p.getTitre(),
                p.getType().name(),
                p.getRevue(),
                p.getAnnee(),
                p.getStatut().name(),
                p.getUrl()
        );
    }

    private boolean isDoctorantActif(Doctorant doctorant) {
        return doctorant != null && doctorant.getStatutDoctorant() == Doctorant.Statut.ACTIF;
    }

    private void updateEntityFromDto(Publication entity, PublicationDTO dto) {
        entity.setTitre(dto.getTitre());
        entity.setType(Publication.TypePublication.valueOf(dto.getType()));
        entity.setRevue(dto.getRevue());
        entity.setAnnee(dto.getAnnee());
        entity.setUrl(dto.getUrl());
        if (dto.getStatut() != null && !dto.getStatut().isBlank()) {
            entity.setStatut(Publication.StatutPublication.valueOf(dto.getStatut()));
        }
    }
}
