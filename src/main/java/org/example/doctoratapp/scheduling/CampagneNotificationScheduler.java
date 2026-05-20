package org.example.doctoratapp.scheduling;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.example.doctoratapp.services.interfaces.IDoctorantService;
import org.example.doctoratapp.services.interfaces.IDossierInscriptionService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CampagneNotificationScheduler {

    private final ICampagneInscriptionService campagneService;
    private final IDoctorantService doctorantService;
    private final IDossierInscriptionService dossierService;
    private final INotificationService notificationService;

    public CampagneNotificationScheduler(ICampagneInscriptionService campagneService,
                                         IDoctorantService doctorantService,
                                         IDossierInscriptionService dossierService,
                                         INotificationService notificationService) {
        this.campagneService = campagneService;
        this.doctorantService = doctorantService;
        this.dossierService = dossierService;
        this.notificationService = notificationService;
    }

    // REGLE CA-03 : Fermeture automatique des campagnes
    @Scheduled(cron = "0 0 0 * * *")
    public void fermerCampagnesExpirees() {
        List<CampagneInscription> campagnesOuvertes = campagneService.findByStatut(CampagneInscription.StatutCampagne.OUVERTE);
        LocalDate aujourdHui = LocalDate.now();

        for (CampagneInscription campagne : campagnesOuvertes) {
            if (campagne.getDateFermeture() != null && campagne.getDateFermeture().isBefore(aujourdHui)) {
                campagne.setStatut(CampagneInscription.StatutCampagne.FERMEE);
                campagneService.modifier(campagne.getId(), campagne);
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void envoyerRappelFermetureCampagnes() {
        LocalDate cible = LocalDate.now().plusDays(7);
        List<CampagneInscription> campagnes = campagneService.findByStatut(CampagneInscription.StatutCampagne.OUVERTE).stream()
                .filter(c -> c.getDateFermeture() != null && c.getDateFermeture().isEqual(cible))
                .collect(Collectors.toList());

        if (campagnes.isEmpty()) {
            return;
        }

        List<Doctorant> doctorantsActifs = doctorantService.findByStatut(Doctorant.Statut.ACTIF);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (CampagneInscription campagne : campagnes) {
            Set<Long> doctorantsAyantDepot = new HashSet<>();
            List<DossierInscription> dossiersCampagne = dossierService.findByCampagne(campagne);
            for (DossierInscription dossier : dossiersCampagne) {
                if (dossier.getDoctorant() != null) {
                    doctorantsAyantDepot.add(dossier.getDoctorant().getId());
                }
            }

            String titre = "RAPPEL : campagne ferme dans 7 jours";
            String message = "RAPPEL : campagne " + campagne.getType() + " ferme dans 7 jours (" + campagne.getDateFermeture().format(formatter) + ").";

            for (Doctorant doctorant : doctorantsActifs) {
                if (doctorantsAyantDepot.contains(doctorant.getId())) {
                    continue;
                }
                if (notificationService.existsByDestinataireAndTitreAndMessage(doctorant, titre, message)) {
                    continue;
                }
                notificationService.envoyer(new Notification(
                        null,
                        titre,
                        message,
                        LocalDateTime.now(),
                        false,
                        Notification.TypeNotification.ALERTE,
                        "/dossiers/nouveau",
                        doctorant
                ));
            }
        }
    }
}
