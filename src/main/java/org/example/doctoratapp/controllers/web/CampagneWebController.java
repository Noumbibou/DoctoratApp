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
import java.time.LocalDate;
import java.util.List;

/**
 * Controller Web pour la gestion des campagnes d'inscription.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /campagnes → templates/campagnes/liste.html (liste des campagnes)</li>
 *   <li>GET /campagnes/ajouter → templates/campagnes/ajouter.html (formulaire création)</li>
 *   <li>POST /campagnes/ajouter → création d'une campagne</li>
 *   <li>GET /campagnes/modifier/{id} → templates/campagnes/modifier.html (formulaire modification)</li>
 *   <li>POST /campagnes/modifier/{id} → modification d'une campagne</li>
 *   <li>POST /campagnes/supprimer/{id} → suppression d'une campagne</li>
 * </ul>
 * </p>
 */
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

    // ══════════════════════════════════════════════
    //  GET /campagnes - Liste des campagnes
    // ══════════════════════════════════════════════

    @GetMapping
    public String listeCampagnes(Model model, Principal principal) {
        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        List<CampagneInscription> campagnes = campagneService.findAll();
        campagnes.forEach(this::actualiserStatutSelonDates);
        model.addAttribute("campagnes", campagnes);

        return "campagnes/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /campagnes/ajouter - Formulaire création
    // ══════════════════════════════════════════════

    @GetMapping("/ajouter")
    public String afficherFormulaireAjout(Model model, Principal principal) {
        if (principal != null) {
            User connectedUser = userService.findByEmail(principal.getName());
            model.addAttribute("connectedUser", connectedUser);
        }

        model.addAttribute("campagneDTO", new CampagneInscriptionDTO());
        return "campagnes/ajouter";
    }

    // ══════════════════════════════════════════════
    //  POST /campagnes/ajouter - Création campagne
    // ══════════════════════════════════════════════

    @PostMapping("/ajouter")
    public String ajouterCampagne(@Valid @ModelAttribute("campagneDTO") CampagneInscriptionDTO campagneDTO,
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

            campagneService.ajouter(campagne);
            redirectAttributes.addFlashAttribute("success", "Campagne créée avec succès");
            return "redirect:/campagnes";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "campagnes/ajouter";
        }
    }

    // ══════════════════════════════════════════════
    //  GET /campagnes/modifier/{id} - Formulaire modification
    // ══════════════════════════════════════════════

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
        dto.setStatut(calculerStatutDynamique(campagne));

        model.addAttribute("campagneDTO", dto);
        model.addAttribute("campagneId", id);
        return "campagnes/modifier";
    }

    // ══════════════════════════════════════════════
    //  POST /campagnes/modifier/{id} - Modification campagne
    // ══════════════════════════════════════════════

    @PostMapping("/modifier/{id}")
    public String modifierCampagne(@PathVariable Long id,
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

            campagneService.modifier(id, campagne);
            redirectAttributes.addFlashAttribute("success", "Campagne modifiée avec succès");
            return "redirect:/campagnes";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("campagneId", id);
            return "campagnes/modifier";
        }
    }

    private CampagneInscription.StatutCampagne calculerStatutDynamique(CampagneInscription campagne) {
        if (campagne == null || campagne.getDateOuverture() == null || campagne.getDateFermeture() == null) {
            return campagne != null ? campagne.getStatut() : CampagneInscription.StatutCampagne.FERMEE;
        }
        LocalDate aujourdhui = LocalDate.now();
        if (!aujourdhui.isBefore(campagne.getDateOuverture()) && !aujourdhui.isAfter(campagne.getDateFermeture())) {
            return CampagneInscription.StatutCampagne.OUVERTE;
        }
        return CampagneInscription.StatutCampagne.FERMEE;
    }

    private void actualiserStatutSelonDates(CampagneInscription campagne) {
        if (campagne != null) {
            campagne.setStatut(calculerStatutDynamique(campagne));
        }
    }

    // ══════════════════════════════════════════════
    //  POST /campagnes/supprimer/{id} - Suppression campagne
    // ══════════════════════════════════════════════

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
