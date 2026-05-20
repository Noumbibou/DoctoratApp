package org.example.doctoratapp.controllers.dev;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Soutenance;
import org.example.doctoratapp.services.interfaces.IDemandeSoutenanceService;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.example.doctoratapp.services.interfaces.ISoutenanceService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Endpoints d'aide pour tests locaux (profil `dev` seulement).
 */
@RestController
@RequestMapping("/dev/soutenances")
@Profile("dev")
public class DevSoutenanceController {

    private final IDemandeSoutenanceService demandeService;
    private final IUserService userService;
    private final ISoutenanceService soutenanceService;

    public DevSoutenanceController(IDemandeSoutenanceService demandeService, IUserService userService, ISoutenanceService soutenanceService) {
        this.demandeService = demandeService;
        this.userService = userService;
        this.soutenanceService = soutenanceService;
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seedDemande(@RequestParam(value = "email", defaultValue = "candidat@doctorat.ma") String email) {
        try {
            org.example.doctoratapp.entities.User u = userService.findByEmail(email);
            Doctorant doc = (Doctorant) u;
            DemandeSoutenance d = new DemandeSoutenance();
            d.setDoctorant(doc);
            DemandeSoutenance saved = demandeService.ajouter(d);
            return ResponseEntity.ok(Map.of("id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/autoriser")
    public ResponseEntity<?> autoriser(@PathVariable Long id) {
        try {
            Soutenance s = soutenanceService.autoriser(id);
            return ResponseEntity.ok(Map.of("soutenanceId", s.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/planifier")
    public ResponseEntity<?> planifier(@PathVariable Long id,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime heure,
                                       @RequestParam String lieu) {
        try {
            DemandeSoutenance demande = demandeService.findById(id);
            Soutenance s = soutenanceService.findByDemandeSoutenance(demande).orElseGet(() -> {
                Soutenance nx = new Soutenance();
                nx.setDemandeSoutenance(demande);
                return nx;
            });
            s.setDateSoutenance(date);
            s.setHeure(heure);
            s.setLieu(lieu);
            soutenanceService.planifier(s);
            demandeService.changerStatut(id, DemandeSoutenance.StatutDemande.PLANIFIEE);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
