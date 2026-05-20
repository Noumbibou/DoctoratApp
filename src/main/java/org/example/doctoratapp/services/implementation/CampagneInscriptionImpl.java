package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Notification;
import org.example.doctoratapp.repo.CampagneInscriptionRepo;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.example.doctoratapp.services.interfaces.IDoctorantService;
import org.example.doctoratapp.services.interfaces.INotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampagneInscriptionImpl implements ICampagneInscriptionService {

    private final CampagneInscriptionRepo campagneRepo;
    private final IDoctorantService doctorantService;
    private final INotificationService notificationService;

    public CampagneInscriptionImpl(CampagneInscriptionRepo campagneRepo,
                                   IDoctorantService doctorantService,
                                   INotificationService notificationService) {
        this.campagneRepo = campagneRepo;
        this.doctorantService = doctorantService;
        this.notificationService = notificationService;
    }

    @Override
    public CampagneInscription findById(Long id) {
        CampagneInscription campagne = campagneRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable avec l'id : " + id));
        return actualiserStatutSelonDates(campagne);
    }

    @Override
    public List<CampagneInscription> findAll() {
        return campagneRepo.findAll().stream()
                .map(this::actualiserStatutSelonDates)
                .collect(Collectors.toList());
    }

    @Override
    public CampagneInscription ajouter(CampagneInscription campagne) {
        if (campagne.getDateFermeture().isBefore(campagne.getDateOuverture())) {
            throw new RuntimeException("La date de fermeture doit être après la date d'ouverture");
        }

        boolean campagneExiste = campagneRepo.findByType(campagne.getType()).stream()
                .filter(existing -> existing.getAnneeUniversitaire().equals(campagne.getAnneeUniversitaire()))
                .anyMatch(existing -> campagneEstOuverte(existing)
                        && campagnesSeChevauchent(campagne, existing));
        if (campagneExiste) {
            throw new RuntimeException("Une campagne ouverte du même type se chevauche pour cette année universitaire.");
        }

        campagne.setStatut(calculerStatutDynamique(campagne));
        CampagneInscription saved = campagneRepo.save(campagne);

        String formattedDate = saved.getDateFermeture().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String titre = "Campagne " + saved.getType() + " ouverte";
        String message = "Campagne " + saved.getType() + " ouverte jusqu'au " + formattedDate;

        List<Doctorant> doctorantsActifs = doctorantService.findByStatut(Doctorant.Statut.ACTIF);
        for (Doctorant doctorant : doctorantsActifs) {
            notificationService.envoyer(new Notification(
                    null,
                    titre,
                    message,
                    LocalDateTime.now(),
                    false,
                    Notification.TypeNotification.INFO,
                    "/dossiers/nouveau",
                    doctorant
            ));
        }

        return saved;
    }

    @Override
    public CampagneInscription modifier(Long id, CampagneInscription campagneModifiee) {
        CampagneInscription existante = findById(id);

        if (campagneModifiee.getDateFermeture().isBefore(campagneModifiee.getDateOuverture())) {
            throw new RuntimeException("La date de fermeture doit être après la date d'ouverture");
        }

        boolean campagneExistanteMemeAnnee = campagneRepo.findByType(campagneModifiee.getType()).stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing -> existing.getAnneeUniversitaire().equals(campagneModifiee.getAnneeUniversitaire()))
                .anyMatch(existing -> campagneEstOuverte(existing)
                        && campagnesSeChevauchent(campagneModifiee, existing));
        if (campagneExistanteMemeAnnee) {
            throw new RuntimeException("Une autre campagne ouverte du même type se chevauche pour cette année universitaire.");
        }

        existante.setDateOuverture(campagneModifiee.getDateOuverture());
        existante.setDateFermeture(campagneModifiee.getDateFermeture());
        existante.setAnneeUniversitaire(campagneModifiee.getAnneeUniversitaire());
        existante.setType(campagneModifiee.getType());

        existante = actualiserStatutSelonDates(existante);
        return campagneRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        CampagneInscription campagne = findById(id);
        if (campagne.getDossiers() != null && !campagne.getDossiers().isEmpty()) {
            throw new RuntimeException("Impossible de supprimer une campagne avec des dossiers associés");
        }
        campagneRepo.deleteById(id);
    }

    @Override
    public List<CampagneInscription> findByStatut(CampagneInscription.StatutCampagne statut) {
        if (statut == CampagneInscription.StatutCampagne.OUVERTE) {
            return campagneRepo.findByDateOuvertureBeforeAndDateFermetureAfter(LocalDate.now(), LocalDate.now()).stream()
                    .map(this::actualiserStatutSelonDates)
                    .collect(Collectors.toList());
        }

        return campagneRepo.findAll().stream()
                .map(this::actualiserStatutSelonDates)
                .filter(c -> c.getStatut() == CampagneInscription.StatutCampagne.FERMEE)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CampagneInscription> findCampagneActive(String anneeUniversitaire) {
        return campagneRepo.findByDateOuvertureBeforeAndDateFermetureAfter(LocalDate.now(), LocalDate.now()).stream()
                .filter(c -> c.getAnneeUniversitaire().equals(anneeUniversitaire))
                .findFirst()
                .map(this::actualiserStatutSelonDates);
    }

    private CampagneInscription actualiserStatutSelonDates(CampagneInscription campagne) {
        if (campagne == null || campagne.getDateOuverture() == null || campagne.getDateFermeture() == null) {
            return campagne;
        }

        CampagneInscription.StatutCampagne statutActuel = calculerStatutDynamique(campagne);
        if (campagne.getStatut() != statutActuel) {
            campagne.setStatut(statutActuel);
            campagneRepo.save(campagne);
        }
        return campagne;
    }

    private CampagneInscription.StatutCampagne calculerStatutDynamique(CampagneInscription campagne) {
        if (campagne == null || campagne.getDateOuverture() == null || campagne.getDateFermeture() == null) {
            return campagne != null ? campagne.getStatut() : CampagneInscription.StatutCampagne.FERMEE;
        }
        if (campagne.getStatut() == CampagneInscription.StatutCampagne.FERMEE) {
            return CampagneInscription.StatutCampagne.FERMEE;
        }

        LocalDate aujourdHui = LocalDate.now();
        if (!aujourdHui.isBefore(campagne.getDateOuverture()) && !aujourdHui.isAfter(campagne.getDateFermeture())) {
            return CampagneInscription.StatutCampagne.OUVERTE;
        }
        return CampagneInscription.StatutCampagne.FERMEE;
    }

    private boolean campagneEstOuverte(CampagneInscription campagne) {
        if (campagne == null || campagne.getDateOuverture() == null || campagne.getDateFermeture() == null) {
            return false;
        }
        LocalDate aujourdHui = LocalDate.now();
        return !aujourdHui.isBefore(campagne.getDateOuverture()) && !aujourdHui.isAfter(campagne.getDateFermeture());
    }

    private boolean campagnesSeChevauchent(CampagneInscription campagne1, CampagneInscription campagne2) {
        if (campagne1 == null || campagne2 == null
                || campagne1.getDateOuverture() == null || campagne1.getDateFermeture() == null
                || campagne2.getDateOuverture() == null || campagne2.getDateFermeture() == null) {
            return false;
        }

        return !campagne1.getDateFermeture().isBefore(campagne2.getDateOuverture())
                && !campagne1.getDateOuverture().isAfter(campagne2.getDateFermeture());
    }
}
