package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.Soutenance;
import org.example.doctoratapp.repo.SoutenanceRepo;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.example.doctoratapp.services.interfaces.ISoutenanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SoutenanceImpl implements ISoutenanceService {

    private final SoutenanceRepo soutenanceRepo;
    private final INotificationService notificationService;
    private final org.example.doctoratapp.repo.DemandeSoutenanceRepo demandeRepo;
    private final org.example.doctoratapp.services.interfaces.IAuditService auditService;

    public SoutenanceImpl(SoutenanceRepo soutenanceRepo,
                          INotificationService notificationService,
                          org.example.doctoratapp.repo.DemandeSoutenanceRepo demandeRepo,
                          org.example.doctoratapp.services.interfaces.IAuditService auditService) {
        this.soutenanceRepo = soutenanceRepo;
        this.notificationService = notificationService;
        this.demandeRepo = demandeRepo;
        this.auditService = auditService;
    }

    @Override
    public Soutenance findById(Long id) {
        return soutenanceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Soutenance introuvable avec l'id : " + id));
    }

    @Override
    public List<Soutenance> findAll() {
        return soutenanceRepo.findAll();
    }

    @Override
    public Soutenance planifier(Soutenance soutenance) {
        return soutenanceRepo.save(soutenance);
    }

    @Override
    public Soutenance modifier(Long id, Soutenance soutenanceModifiee) {
        Soutenance existante = findById(id);
        existante.setDateSoutenance(soutenanceModifiee.getDateSoutenance());
        existante.setHeure(soutenanceModifiee.getHeure());
        existante.setLieu(soutenanceModifiee.getLieu());
        return soutenanceRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!soutenanceRepo.existsById(id)) {
            throw new RuntimeException("Soutenance introuvable avec l'id : " + id);
        }
        soutenanceRepo.deleteById(id);
    }

    @Override
    public Optional<Soutenance> findByDemandeSoutenance(DemandeSoutenance demande) {
        return soutenanceRepo.findByDemandeSoutenance(demande);
    }

    @Override
    public List<Soutenance> findByPeriode(LocalDate debut, LocalDate fin) {
        return soutenanceRepo.findByDateSoutenanceBetween(debut, fin);
    }

    @Override
    public Soutenance autoriser(Long id) {
        DemandeSoutenance demande = demandeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de soutenance introuvable avec l'id : " + id));

        demande.setStatut(DemandeSoutenance.StatutDemande.AUTORISEE);
        demandeRepo.save(demande);
        try {
            auditService.record("DemandeSoutenance", demande.getId(), "autoriser", "autorisé par admin");
        } catch (Exception e) {
            System.err.println("Audit error: " + e.getMessage());
        }

        Soutenance soutenance = soutenanceRepo.findByDemandeSoutenance(demande)
                .orElseGet(() -> {
                    Soutenance s = new Soutenance();
                    s.setDemandeSoutenance(demande);
                    return s;
                });

        soutenance.setAutorisationAdmin(true);
        Soutenance saved = soutenanceRepo.save(soutenance);

        // Notifier le doctorant
        notificationService.envoyer(new Notification(
                null,
                "Soutenance",
                "Votre demande de soutenance a été autorisée par l'administration. Veuillez procéder à la planification.",
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/soutenances/liste",
                demande.getDoctorant()
        ));

        return saved;
    }
}
