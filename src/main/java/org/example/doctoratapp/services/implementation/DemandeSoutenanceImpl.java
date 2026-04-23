package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.*;
import org.example.doctoratapp.repo.DemandeSoutenanceRepo;
import org.example.doctoratapp.services.interfaces.IDemandeSoutenanceService;
import org.example.doctoratapp.services.interfaces.IFormationDoctoraleService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.example.doctoratapp.services.interfaces.IPublicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DemandeSoutenanceImpl implements IDemandeSoutenanceService {
    private DemandeSoutenanceRepo demandeRepo;
    private IPublicationService publicationService;
    private IFormationDoctoraleService formationService;
    private INotificationService notificationService;

    public DemandeSoutenanceImpl(DemandeSoutenanceRepo demandeRepo, IPublicationService publicationService, IFormationDoctoraleService formationService, INotificationService notificationService) {
        this.demandeRepo = demandeRepo;
        this.publicationService = publicationService;
        this.formationService = formationService;
        this.notificationService = notificationService;
    }

    @Override
    public boolean isPrerequísRemplis(DemandeSoutenance demandeSoutenance, List<Publication> publications, int totalHeuresFormation) {
        long journaux = publications.stream()
                .filter(p -> p.getType() == Publication.TypePublication.JOURNAL_Q1
                        || p.getType() == Publication.TypePublication.JOURNAL_Q2)
                .count();

        long conferences = publications.stream()
                .filter(p -> p.getType() == Publication.TypePublication.CONFERENCE)
                .count();

        return journaux >= 2 && conferences >= 2 && totalHeuresFormation >= 200;
    }

    @Override
    public DemandeSoutenance findById(Long id) {
        return demandeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec l'id : " + id));
    }

    @Override
    public List<DemandeSoutenance> findAll() {
        return demandeRepo.findAll();
    }

    @Override
    public DemandeSoutenance ajouter(DemandeSoutenance demande) {
        // Vérifier les prérequis avant de soumettre
        if (!verifierTousPrerequis(demande.getDoctorant())) {
            List<String> manquants = getPrerequísNonRemplis(demande.getDoctorant());
            throw new RuntimeException("Prérequis non remplis : " + manquants);
        }
        demande.setDateDepot(LocalDate.now());
        demande.setStatut(DemandeSoutenance.StatutDemande.SOUMIS);
        return demandeRepo.save(demande);
    }

    @Override
    public DemandeSoutenance modifier(Long id, DemandeSoutenance demandeModifiee) {
        DemandeSoutenance existante = findById(id);
        existante.setCommentaire(demandeModifiee.getCommentaire());
        return demandeRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!demandeRepo.existsById(id)) {
            throw new RuntimeException("Demande introuvable avec l'id : " + id);
        }
        demandeRepo.deleteById(id);
    }

    @Override
    public List<DemandeSoutenance> findByDoctorant(Doctorant doctorant) {
        return demandeRepo.findByDoctorant(doctorant);
    }

    @Override
    public List<DemandeSoutenance> findByStatut(DemandeSoutenance.StatutDemande statut) {
        return demandeRepo.findByStatut(statut);
    }

    @Override
    public DemandeSoutenance changerStatut(Long id, DemandeSoutenance.StatutDemande statut) {
        DemandeSoutenance demande = findById(id);
        demande.setStatut(statut);

        notificationService.envoyer(new Notification(
                null,
                "Votre demande de soutenance est maintenant : " + statut,
                LocalDateTime.now(),
                false,
                Notification.TypeNotification.INFO,
                "/soutenances/" + id,
                demande.getDoctorant()
        ));

        return demandeRepo.save(demande);
    }

    @Override
    public boolean verifierTousPrerequis(Doctorant doctorant) {
        return publicationService.prerequisPublicationsRemplis(doctorant)
                && formationService.prerequisFormationRemplis(doctorant);
    }

    @Override
    public List<String> getPrerequísNonRemplis(Doctorant doctorant) {
        List<String> manquants = new ArrayList<>();
        if (!publicationService.prerequisPublicationsRemplis(doctorant)) {
            manquants.add("2 journaux Q1/Q2 et 2 conférences requis");
        }
        if (!formationService.prerequisFormationRemplis(doctorant)) {
            manquants.add("200h de formation doctorale requises");
        }
        return manquants;
    }
}
