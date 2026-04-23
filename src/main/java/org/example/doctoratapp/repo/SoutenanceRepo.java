package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Soutenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoutenanceRepo extends JpaRepository<Soutenance, Long> {

    // Trouver par demande de soutenance
    Optional<Soutenance> findByDemandeSoutenance(DemandeSoutenance demande);

    // Soutenances planifiées entre deux dates
    List<Soutenance> findByDateSoutenanceBetween(LocalDate debut, LocalDate fin);

    // Soutenances autorisées
    List<Soutenance> findByAutorisationAdmin(Boolean autorisationAdmin);
}
