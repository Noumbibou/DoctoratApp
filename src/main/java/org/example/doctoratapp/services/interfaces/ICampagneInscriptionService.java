package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.CampagneInscription;

import java.util.List;
import java.util.Optional;

public interface ICampagneInscriptionService {

    CampagneInscription findById(Long id);
    List<CampagneInscription> findAll();
    CampagneInscription ajouter(CampagneInscription campagne);
    CampagneInscription modifier(Long id, CampagneInscription campagne);
    void supprimer(Long id);
    List<CampagneInscription> findByStatut(CampagneInscription.StatutCampagne statut);
    Optional<CampagneInscription> findCampagneActive(String anneeUniversitaire);
}
