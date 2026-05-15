package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import org.example.doctoratapp.dto.jury.MembreJuryDTO;
import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.MembreJury;
import org.example.doctoratapp.entities.Soutenance;
import org.example.doctoratapp.services.interfaces.IDemandeSoutenanceService;
import org.example.doctoratapp.services.interfaces.IMembreJuryService;
import org.example.doctoratapp.services.interfaces.ISoutenanceService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controller Web pour la gestion des membres du jury.
 *
 * <p>Pages gérées :
 * <ul>
 *   <li>GET /jury/soutenance/{id} → Liste des membres pour une soutenance</li>
 *   <li>GET /jury/nouveau/{soutenanceId} → Formulaire d'ajout</li>
 *   <li>POST /jury/nouveau → Enregistrement</li>
 *   <li>POST /jury/{id}/supprimer → Retrait d'un membre</li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/jury")
public class JuryWebController {

    private final IMembreJuryService juryService;
    private final ISoutenanceService soutenanceService;
    private final IDemandeSoutenanceService demandeService;
    private final IUserService userService;

    public JuryWebController(IMembreJuryService juryService,
                             ISoutenanceService soutenanceService,
                             IDemandeSoutenanceService demandeService,
                             IUserService userService) {
        this.juryService = juryService;
        this.soutenanceService = soutenanceService;
        this.demandeService = demandeService;
        this.userService = userService;
    }

    // ══════════════════════════════════════════════
    //  GET /jury/soutenance/{id}
    // ══════════════════════════════════════════════

    @GetMapping("/soutenance/{id}")
    public String listeJury(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        Soutenance soutenance = soutenanceService.findById(id);
        DemandeSoutenance demande = soutenance.getDemandeSoutenance();

        List<MembreJury> jury = juryService.findByDemandeSoutenance(demande);

        model.addAttribute("jury", jury);
        model.addAttribute("soutenanceId", id);
        model.addAttribute("connectedUser", userService.findByEmail(principal.getName()));

        return "jury/liste";
    }

    // ══════════════════════════════════════════════
    //  GET /jury/nouveau/{soutenanceId}
    // ══════════════════════════════════════════════

    @GetMapping("/nouveau/{soutenanceId}")
    public String afficherFormulaireAjout(@PathVariable Long soutenanceId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        model.addAttribute("soutenanceId", soutenanceId);
        model.addAttribute("membreJury", new MembreJuryDTO());
        return "jury/formulaire";
    }

    @PostMapping("/nouveau")
    public String ajouterMembre(@Valid @ModelAttribute("membreJury") MembreJuryDTO dto,
                                BindingResult result,
                                @RequestParam Long soutenanceId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "jury/formulaire";
        }

        Soutenance soutenance = soutenanceService.findById(soutenanceId);
        DemandeSoutenance demande = soutenance.getDemandeSoutenance();

        MembreJury membre = new MembreJury();
        membre.setNom(dto.getNom());
        membre.setPrenom(dto.getPrenom());
        membre.setGrade(dto.getGrade());
        membre.setEtablissement(dto.getEtablissement());
        membre.setRole(MembreJury.RoleJury.valueOf(dto.getRole()));
        membre.setDemandeSoutenance(demande);

        juryService.ajouter(membre);

        redirectAttributes.addFlashAttribute("successMessage", "Membre ajouté au jury avec succès.");
        return "redirect:/jury/soutenance/" + soutenanceId;
    }

    // ══════════════════════════════════════════════
    //  POST /jury/{id}/supprimer
    // ══════════════════════════════════════════════

    @PostMapping("/{id}/supprimer")
    public String supprimerMembre(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        MembreJury membre = juryService.findById(id);
        Long demandeId = membre.getDemandeSoutenance().getId();
        
        // Retrouver la soutenance liée à cette demande pour la redirection
        Soutenance soutenance = soutenanceService.findAll().stream()
                .filter(s -> s.getDemandeSoutenance().getId().equals(demandeId))
                .findFirst()
                .orElse(null);

        juryService.supprimer(id);

        redirectAttributes.addFlashAttribute("successMessage", "Membre retiré du jury.");
        
        if (soutenance != null) {
            return "redirect:/jury/soutenance/" + soutenance.getId();
        }
        return "redirect:/dashboard";
    }
}
