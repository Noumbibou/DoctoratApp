package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.Derogation;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DerogationRepo extends JpaRepository<Derogation, Long> {

    // Dérogations d'un doctorant
    List<Derogation> findByDoctorant(Doctorant doctorant);

    // Dérogations par statut
    List<Derogation> findByStatut(Derogation.StatutDerogation statut);

    // Dérogations accordées par un admin
    List<Derogation> findByAccordeePar(User admin);

    // Dernière dérogation d'un doctorant
    Optional<Derogation> findTopByDoctorantOrderByDateDemandeDesc(Doctorant doctorant);
}
