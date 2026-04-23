package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.repo.CampagneInscriptionRepo;
import org.example.doctoratapp.services.interfaces.ICampagneInscriptionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CampagneInscriptionImpl implements ICampagneInscriptionService {

    private CampagneInscriptionRepo campagneRepo;

    public CampagneInscriptionImpl(CampagneInscriptionRepo campagneRepo) {
        this.campagneRepo = campagneRepo;
    }

    @Override
    public CampagneInscription findById(Long id) {
        return campagneRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable avec l'id : " + id));
    }

    @Override
    public List<CampagneInscription> findAll() {
        return campagneRepo.findAll();
    }

    @Override
    public CampagneInscription ajouter(CampagneInscription campagne) {
        // Vérifier que la date de fermeture est après l'ouverture
        if (campagne.getDateFermeture().isBefore(campagne.getDateOuverture())) {
            throw new RuntimeException("La date de fermeture doit être après la date d'ouverture");
        }
        return campagneRepo.save(campagne);
    }

    @Override
    public CampagneInscription modifier(Long id, CampagneInscription campagneModifiee) {
        CampagneInscription existante = findById(id);
        existante.setDateOuverture(campagneModifiee.getDateOuverture());
        existante.setDateFermeture(campagneModifiee.getDateFermeture());
        existante.setStatut(campagneModifiee.getStatut());
        return campagneRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!campagneRepo.existsById(id)) {
            throw new RuntimeException("Campagne introuvable avec l'id : " + id);
        }
        campagneRepo.deleteById(id);
    }

    @Override
    public List<CampagneInscription> findByStatut(CampagneInscription.StatutCampagne statut) {
        return campagneRepo.findByStatut(statut);
    }

    @Override
    public Optional<CampagneInscription> findCampagneActive(String anneeUniversitaire) {
        return campagneRepo.findByAnneeUniversitaireAndStatut(
                anneeUniversitaire,
                CampagneInscription.StatutCampagne.OUVERTE
        );
    }
}
