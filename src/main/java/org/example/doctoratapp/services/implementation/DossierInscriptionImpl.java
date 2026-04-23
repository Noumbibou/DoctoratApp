package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.repo.DossierInscriptionRepo;
import org.example.doctoratapp.services.interfaces.IDossierInscriptionService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DossierInscriptionImpl implements IDossierInscriptionService {

    private DossierInscriptionRepo dossierRepo;
    INotificationService notificationService;

    public DossierInscriptionImpl(DossierInscriptionRepo dossierRepo, INotificationService notificationService) {
        this.dossierRepo = dossierRepo;
        this.notificationService = notificationService;
    }

    @Override
    public DossierInscription findById(Long id) {
        return dossierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable avec l'id : " + id));
    }

    @Override
    public List<DossierInscription> findAll() {
        return dossierRepo.findAll();
    }

    @Override
    public DossierInscription ajouter(DossierInscription dossier) {
        dossier.setDateDepot(LocalDate.now());
        dossier.setStatut(DossierInscription.StatutDossier.SOUMIS);
        DossierInscription sauvegarde = dossierRepo.save(dossier);

        // Notifier le directeur
        notificationService.envoyer(new Notification(
                null,
                "Nouveau dossier à valider pour " + dossier.getDoctorant().getNom(),
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.ACTION_REQUISE,
                "/dossiers/" + sauvegarde.getId(),
                dossier.getDirecteurThese()
        ));

        return sauvegarde;
    }

    @Override
    public DossierInscription modifier(Long id, DossierInscription dossierModifie) {
        DossierInscription existant = findById(id);
        existant.setSujetThese(dossierModifie.getSujetThese());
        existant.setDirecteurThese(dossierModifie.getDirecteurThese());
        return dossierRepo.save(existant);
    }

    @Override
    public void supprimer(Long id) {
        if (!dossierRepo.existsById(id)) {
            throw new RuntimeException("Dossier introuvable avec l'id : " + id);
        }
        dossierRepo.deleteById(id);
    }

    @Override
    public List<DossierInscription> findByDoctorant(Doctorant doctorant) {
        return dossierRepo.findByDoctorant(doctorant);
    }

    @Override
    public List<DossierInscription> findByStatut(DossierInscription.StatutDossier statut) {
        return dossierRepo.findByStatut(statut);
    }

    @Override
    public List<DossierInscription> findByDirecteur(DirecteurThese directeur) {
        return dossierRepo.findByDirecteurThese(directeur);
    }

    @Override
    public DossierInscription changerStatut(Long id, DossierInscription.StatutDossier statut) {
        DossierInscription dossier = findById(id);
        dossier.setStatut(statut);

        // Notifier le doctorant du changement de statut
        notificationService.envoyer(new Notification(
                null,
                "Votre dossier est maintenant : " + statut,
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/dossiers/" + id,
                dossier.getDoctorant()
        ));

        return dossierRepo.save(dossier);
    }
}
