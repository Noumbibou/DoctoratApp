package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.MembreJury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembreJuryRepo extends JpaRepository<MembreJury, Long> {

    // Membres d'une demande de soutenance
    List<MembreJury> findByDemandeSoutenance(DemandeSoutenance demande);

    // Membres par rôle dans une demande
    List<MembreJury> findByDemandeSoutenanceAndRole(
            DemandeSoutenance demande,
            MembreJury.RoleJury role
    );
}
