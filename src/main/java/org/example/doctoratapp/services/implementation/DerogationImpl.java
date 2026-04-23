package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.Derogation;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.repo.DerogationRepo;
import org.example.doctoratapp.services.interfaces.IDerogationService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DerogationImpl implements IDerogationService {

    private DerogationRepo derogationRepo;
    private INotificationService notificationService;

    public DerogationImpl(DerogationRepo derogationRepo, INotificationService notificationService) {
        this.derogationRepo = derogationRepo;
        this.notificationService = notificationService;
    }

    @Override
    public Derogation findById(Long id) {
        return derogationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dérogation introuvable avec l'id : " + id));
    }

    @Override
    public List<Derogation> findAll() {
        return derogationRepo.findAll();
    }

    @Override
    public Derogation ajouter(Derogation derogation) {
        derogation.setDateDemande(LocalDate.now());
        derogation.setStatut(Derogation.StatutDerogation.EN_ATTENTE);
        return derogationRepo.save(derogation);
    }

    @Override
    public Derogation modifier(Long id, Derogation derogationModifiee) {
        Derogation existante = findById(id);
        existante.setMotif(derogationModifiee.getMotif());
        return derogationRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!derogationRepo.existsById(id)) {
            throw new RuntimeException("Dérogation introuvable avec l'id : " + id);
        }
        derogationRepo.deleteById(id);
    }

    @Override
    public List<Derogation> findByDoctorant(Doctorant doctorant) {
        return derogationRepo.findByDoctorant(doctorant);
    }

    @Override
    public List<Derogation> findByStatut(Derogation.StatutDerogation statut) {
        return derogationRepo.findByStatut(statut);
    }

    @Override
    public Derogation accorder(Long id, User admin) {
        Derogation derogation = findById(id);
        derogation.setStatut(Derogation.StatutDerogation.ACCORDEE);
        derogation.setAccordeePar(admin);

        // Notifier le doctorant
        notificationService.envoyer(new Notification(
                null,
                "Votre demande de dérogation a été accordée",
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/derogations/" + id,
                derogation.getDoctorant()
        ));

        return derogationRepo.save(derogation);
    }

    @Override
    public Derogation refuser(Long id, User admin) {
        Derogation derogation = findById(id);
        derogation.setStatut(Derogation.StatutDerogation.REFUSEE);
        derogation.setAccordeePar(admin);

        // Notifier le doctorant
        notificationService.envoyer(new Notification(
                null,
                "Votre demande de dérogation a été refusée",
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.ALERTE,
                "/derogations/" + id,
                derogation.getDoctorant()
        ));

        return derogationRepo.save(derogation);
    }
}
