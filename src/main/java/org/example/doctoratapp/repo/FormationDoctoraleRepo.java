package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.FormationDoctorale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FormationDoctoraleRepo extends JpaRepository<FormationDoctorale, Long> {

    // Formations d'un doctorant
    List<FormationDoctorale> findByDoctorant(Doctorant doctorant);

    // Total des heures de formation d'un doctorant
    @Query("SELECT COALESCE(SUM(f.heures), 0) FROM FormationDoctorale f WHERE f.doctorant = :doctorant")
    int sumHeuresByDoctorant(@Param("doctorant") Doctorant doctorant);

    // Formations entre deux dates
    List<FormationDoctorale> findByDoctorantAndDateFormationBetween(
            Doctorant doctorant,
            LocalDate debut,
            LocalDate fin
    );
}
