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

    private SoutenanceRepo soutenanceRepo;
    private INotificationService notificationService;

    public SoutenanceImpl(SoutenanceRepo soutenanceRepo, INotificationService notificationService) {
        this.soutenanceRepo = soutenanceRepo;
        this.notificationService = notificationService;
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
        Soutenance soutenance = findById(id);
        soutenance.setAutorisationAdmin(true);

        // Notifier le doctorant
        notificationService.envoyer(new Notification(
                null,
                "Votre soutenance est autorisée le " + soutenance.getDateSoutenance()
                        + " à " + soutenance.getLieu(),
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/soutenances/" + id,
                soutenance.getDemandeSoutenance().getDoctorant()
        ));

        return soutenanceRepo.save(soutenance);
    }
}
