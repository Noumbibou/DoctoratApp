package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import org.example.doctoratapp.dto.campagne.CampagneInscriptionDTO;
import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/campagnes")
public class CampagneWebController {

    private final ICampagneInscriptionService campagneService;
    private final IUserService userService;

    public CampagneWebController(ICampagneInscriptionService campagneService,
                                 IUserService userService) {
        this.campagneService = campagneService;
        this.userService = userService;
    }

    // ✅ ===============================
    // ✅ LISTE DES CAMPAGNES (CORRIGÉ)
    // ✅ ===============================
    @GetMapping
    public String listeCampagnes(Model model, Principal principal) {

        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        // ✅ IMPORTANT : on ne recalcule PAS le statut
        List<CampagneInscription> campagnes = campagneService.findAll();

        model.addAttribute("campagnes", campagnes);

        return "campagnes/liste";
    }

    // ✅ ===============================
    // ✅ FORMULAIRE AJOUT
    // ✅ ===============================
    @GetMapping("/ajouter")
    public String afficherFormulaireAjout(Model model, Principal principal) {

        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        model.addAttribute("campagneDTO", new CampagneInscriptionDTO());
        return "campagnes/ajouter";
    }

    // ✅ ===============================
    // ✅ CREATION CAMPAGNE
    // ✅ ===============================
    @PostMapping("/ajouter")
    public String ajouterCampagne(
            @Valid @ModelAttribute("campagneDTO") CampagneInscriptionDTO campagneDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        if (bindingResult.hasErrors()) {
            return "campagnes/ajouter";
        }

        try {
            CampagneInscription campagne = new CampagneInscription();
            campagne.setDateOuverture(campagneDTO.getDateOuverture());
            campagne.setDateFermeture(campagneDTO.getDateFermeture());
            campagne.setAnneeUniversitaire(campagneDTO.getAnneeUniversitaire());
            campagne.setType(campagneDTO.getType());

            // ✅ TOUJOURS OUVERTE À LA CRÉATION
            campagne.setStatut(CampagneInscription.StatutCampagne.OUVERTE);

            campagneService.ajouter(campagne);

            redirectAttributes.addFlashAttribute("success", "Campagne créée avec succès");
            return "redirect:/campagnes";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "campagnes/ajouter";
        }
    }

    // ✅ ===============================
    // ✅ FORMULAIRE MODIFICATION
    // ✅ ===============================
    @GetMapping("/modifier/{id}")
    public String afficherFormulaireModification(@PathVariable Long id,
                                                 Model model,
                                                 Principal principal) {

        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        CampagneInscription campagne = campagneService.findById(id);

        CampagneInscriptionDTO dto = new CampagneInscriptionDTO();
        dto.setDateOuverture(campagne.getDateOuverture());
        dto.setDateFermeture(campagne.getDateFermeture());
        dto.setAnneeUniversitaire(campagne.getAnneeUniversitaire());
        dto.setType(campagne.getType());

        // ✅ IMPORTANT : on utilise le statut DB
        dto.setStatut(campagne.getStatut());

        model.addAttribute("campagneDTO", dto);
        model.addAttribute("campagneId", id);
        return "campagnes/modifier";
    }

    // ✅ ===============================
    // ✅ MODIFICATION
    // ✅ ===============================
    @PostMapping("/modifier/{id}")
    public String modifierCampagne(
            @PathVariable Long id,
            @Valid @ModelAttribute("campagneDTO") CampagneInscriptionDTO campagneDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("campagneId", id);
            return "campagnes/modifier";
        }

        try {
            CampagneInscription campagne = new CampagneInscription();
            campagne.setDateOuverture(campagneDTO.getDateOuverture());
            campagne.setDateFermeture(campagneDTO.getDateFermeture());
            campagne.setAnneeUniversitaire(campagneDTO.getAnneeUniversitaire());
            campagne.setType(campagneDTO.getType());

            // ✅ ON GARDE LE STATUT EXISTANT
            campagne.setStatut(campagneDTO.getStatut());

            campagneService.modifier(id, campagne);

            redirectAttributes.addFlashAttribute("success", "Campagne modifiée avec succès");
            return "redirect:/campagnes";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("campagneId", id);
            return "campagnes/modifier";
        }
    }

    // ✅ ===============================
    // ✅ SUPPRESSION
    // ✅ ===============================
    @PostMapping("/supprimer/{id}")
    public String supprimerCampagne(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            campagneService.supprimer(id);
            redirectAttributes.addFlashAttribute("success", "Campagne supprimée avec succès");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/campagnes";
    }
}