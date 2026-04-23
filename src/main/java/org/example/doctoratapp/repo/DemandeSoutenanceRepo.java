package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Doctorant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeSoutenanceRepo extends JpaRepository<DemandeSoutenance, Long> {

    // Demandes d'un doctorant
    List<DemandeSoutenance> findByDoctorant(Doctorant doctorant);

    // Demandes par statut
    List<DemandeSoutenance> findByStatut(DemandeSoutenance.StatutDemande statut);

    // Dernière demande d'un doctorant
    Optional<DemandeSoutenance> findTopByDoctorantOrderByDateDepotDesc(Doctorant doctorant);

    // Demande d'un doctorant par statut
    Optional<DemandeSoutenance> findByDoctorantAndStatut(
            Doctorant doctorant,
            DemandeSoutenance.StatutDemande statut
    );
}
