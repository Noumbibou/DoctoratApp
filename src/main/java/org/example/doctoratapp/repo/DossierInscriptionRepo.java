package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.CampagneInscription;
import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.DossierInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierInscriptionRepo extends JpaRepository<DossierInscription, Long> {
    // Tous les dossiers d'un doctorant
    List<DossierInscription> findByDoctorant(Doctorant doctorant);

    // Dossiers par statut
    List<DossierInscription> findByStatut(DossierInscription.StatutDossier statut);

    // Dossiers d'un directeur
    List<DossierInscription> findByDirecteurThese(DirecteurThese directeurThese);

    // Dossiers d'un doctorant par statut
    List<DossierInscription> findByDoctorantAndStatut(
            Doctorant doctorant,
            DossierInscription.StatutDossier statut
    );

    // Dossiers d'une campagne
    List<DossierInscription> findByCampagne(CampagneInscription campagne);
}
