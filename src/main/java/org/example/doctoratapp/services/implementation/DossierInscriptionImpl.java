package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.repo.DossierInscriptionRepo;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.example.doctoratapp.services.interfaces.IDossierInscriptionService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DossierInscriptionImpl implements IDossierInscriptionService {

    private final DossierInscriptionRepo dossierRepo;
    private final INotificationService notificationService;
    private final ICampagneInscriptionService campagneService;
    private final org.example.doctoratapp.services.interfaces.IAuditService auditService;

    public DossierInscriptionImpl(DossierInscriptionRepo dossierRepo,
                                  INotificationService notificationService,
                                  ICampagneInscriptionService campagneService,
                                  org.example.doctoratapp.services.interfaces.IAuditService auditService) {
        this.dossierRepo = dossierRepo;
        this.notificationService = notificationService;
        this.campagneService = campagneService;
        this.auditService = auditService;
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
        if (dossier.getDoctorant() == null) {
            throw new RuntimeException("Doctorant requis pour la soumission du dossier.");
        }

        if (hasActiveDossier(dossier.getDoctorant())) {
            throw new RuntimeException("Vous avez déjà un dossier en cours de traitement.");
        }

        if (dossier.getCampagne() == null || dossier.getCampagne().getId() == null) {
            throw new RuntimeException("Campagne d'inscription requise.");
        }

        CampagneInscription campagne = campagneService.findById(dossier.getCampagne().getId());
        if (campagne.getStatut() != CampagneInscription.StatutCampagne.OUVERTE) {
            throw new RuntimeException("La campagne d'inscription doit être ouverte.");
        }

        dossier.setCampagne(campagne);
        dossier.setDateDepot(LocalDate.now());
        dossier.setStatut(DossierInscription.StatutDossier.SOUMIS);
        DossierInscription sauvegarde = dossierRepo.save(dossier);

        // Notifier le directeur
        notificationService.envoyer(new Notification(
                null,
                "Inscription",
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
    public boolean hasActiveDossier(Doctorant doctorant) {
        return !dossierRepo.findByDoctorantAndStatutIn(doctorant, List.of(
                DossierInscription.StatutDossier.SOUMIS,
                DossierInscription.StatutDossier.EN_ATTENTE_DIRECTEUR,
                DossierInscription.StatutDossier.EN_ATTENTE_ADMIN
        )).isEmpty();
    }

    @Override
    public DossierInscription modifier(Long id, DossierInscription dossierModifie) {
        DossierInscription existant = findById(id);
        existant.setSujetThese(dossierModifie.getSujetThese());
        existant.setDirecteurThese(dossierModifie.getDirecteurThese());
        existant.setStatut(dossierModifie.getStatut());
        existant.setCommentaire(dossierModifie.getCommentaire());
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
    public List<DossierInscription> findByCampagne(CampagneInscription campagne) {
        return dossierRepo.findByCampagne(campagne);
    }

    @Override
    public DossierInscription changerStatut(Long id, DossierInscription.StatutDossier statut) {
        DossierInscription dossier = findById(id);
        DossierInscription.StatutDossier old = dossier.getStatut();
        dossier.setStatut(statut);

        // Notifier le doctorant du changement de statut
        notificationService.envoyer(new Notification(
                null,
                "Inscription",
                "Votre dossier est maintenant : " + statut,
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/dossiers/" + id,
                dossier.getDoctorant()
        ));

        DossierInscription saved = dossierRepo.save(dossier);

        // Audit minimal
        try {
            auditService.record("DossierInscription", saved.getId(), "changerStatut", "from=" + old + " to=" + statut);
        } catch (Exception e) {
            // Ne pas bloquer l'opération pour un échec d'audit
            System.err.println("Audit error: " + e.getMessage());
        }

        return saved;
    }
}
