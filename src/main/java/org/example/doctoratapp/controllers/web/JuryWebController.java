package org.example.doctoratapp.controllers.web;

import jakarta.validation.Valid;
import org.example.doctoratapp.dto.jury.MembreJuryDTO;
import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.services.interfaces.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

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
    private final IDossierInscriptionService dossierService;

    public JuryWebController(IMembreJuryService juryService,
                             ISoutenanceService soutenanceService,
                             IDemandeSoutenanceService demandeService,
                             IUserService userService,
                             IDossierInscriptionService dossierService) {
        this.juryService = juryService;
        this.soutenanceService = soutenanceService;
        this.demandeService = demandeService;
        this.userService = userService;
        this.dossierService = dossierService;
    }

    // ══════════════════════════════════════════════
    //  GET /jury/soutenance/{id}
    // ══════════════════════════════════════════════

    @GetMapping("/soutenance/{id}")
    public String listeJury(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User connectedUser = userService.findByEmail(principal.getName());
        model.addAttribute("connectedUser", connectedUser);

        if (id == 0) {
            List<DemandeSoutenance> demandesList;
            if (connectedUser.getRole() == User.Role.ADMIN) {
                demandesList = demandeService.findAll();
            } else if (connectedUser.getRole() == User.Role.DIRECTEUR) {
                DirecteurThese directeur = (DirecteurThese) connectedUser;
                List<DossierInscription> dossiers = dossierService.findByDirecteur(directeur);
                List<Doctorant> doctorants = dossiers.stream()
                        .map(DossierInscription::getDoctorant)
                        .distinct()
                        .collect(Collectors.toList());
                demandesList = doctorants.stream()
                        .flatMap(doc -> demandeService.findByDoctorant(doc).stream())
                        .collect(Collectors.toList());
            } else {
                return "redirect:/dashboard";
            }

            List<org.example.doctoratapp.controllers.web.SoutenanceWebController.SoutenanceView> views = demandesList.stream()
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
                        // Fallback subject resolution if the map lambda has a typo
                        if (dossiers != null && !dossiers.isEmpty() && dossiers.get(0).getSujetThese() != null) {
                            sujet = dossiers.get(0).getSujetThese();
                        }
                        return new org.example.doctoratapp.controllers.web.SoutenanceWebController.SoutenanceView(
                                d.getId(),
                                d.getDoctorant().getNom() + " " + d.getDoctorant().getPrenom(),
                                sujet,
                                d.getSoutenance() != null ? d.getSoutenance().getDateSoutenance() : null,
                                d.getSoutenance() != null && d.getSoutenance().getHeure() != null ? d.getSoutenance().getHeure().toString() : null,
                                d.getSoutenance() != null ? d.getSoutenance().getLieu() : "Non planifiée",
                                d.getStatut().name(),
                                d.getDateDepot(),
                                dossiers != null && !dossiers.isEmpty() && dossiers.get(0).getDirecteurThese() != null
                                        ? dossiers.get(0).getDirecteurThese().getNom() + " " + dossiers.get(0).getDirecteurThese().getPrenom()
                                        : "Non spécifié"
                        );
                    })
                    .collect(Collectors.toList());

            model.addAttribute("demandes", views);
            return "jury/choix_soutenance";
        }

        DemandeSoutenance demande = demandeService.findById(id);
        List<MembreJury> jury = juryService.findByDemandeSoutenance(demande);

        model.addAttribute("jury", jury);
        model.addAttribute("soutenanceId", id);

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

        DemandeSoutenance demande = demandeService.findById(soutenanceId);

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

        juryService.supprimer(id);

        redirectAttributes.addFlashAttribute("successMessage", "Membre retiré du jury.");
        return "redirect:/jury/soutenance/" + demandeId;
    }
}
