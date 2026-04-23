package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.Doctorant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorantRepo extends JpaRepository<Doctorant, Long> {

    // Trouver par numéro d'inscription
    Optional<Doctorant> findByNumInscription(String numInscription);

    // Trouver par statut
    List<Doctorant> findByStatutDoctorant(Doctorant.Statut statutDoctorant);

    // Trouver les doctorants d'un directeur
    List<Doctorant> findByDossiers_DirecteurThese_Id(Long directeurId);

    // Trouver les doctorants inscrits avant une date (règle 3/6 ans)
    List<Doctorant> findByDateInscriptionInitialeBefore(LocalDate date);
}
