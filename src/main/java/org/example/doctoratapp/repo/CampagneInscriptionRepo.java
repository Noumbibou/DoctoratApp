package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.CampagneInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampagneInscriptionRepo extends JpaRepository<CampagneInscription, Long> {

    // Trouver les campagnes par type
    List<CampagneInscription> findByType(CampagneInscription.TypeCampagne type);

    // Trouver les campagnes ouvertes
    List<CampagneInscription> findByStatut(CampagneInscription.StatutCampagne statut);

    // Trouver la campagne active par année universitaire
    Optional<CampagneInscription> findByAnneeUniversitaireAndStatut(
            String anneeUniversitaire,
            CampagneInscription.StatutCampagne statut
    );

    // Campagnes ouvertes entre deux dates
    List<CampagneInscription> findByDateOuvertureBeforeAndDateFermetureAfter(
            LocalDate maintenant1,
            LocalDate maintenant2
    );
}
