package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepo extends JpaRepository<Publication, Long> {

    // Publications d'un doctorant
    List<Publication> findByDoctorant(Doctorant doctorant);

    // Publications d'un doctorant par type
    List<Publication> findByDoctorantAndType(
            Doctorant doctorant,
            Publication.TypePublication type
    );

    // Publications d'un doctorant par statut
    List<Publication> findByDoctorantAndStatut(
            Doctorant doctorant,
            Publication.StatutPublication statut
    );

    // Compter les publications d'un doctorant par type
    long countByDoctorantAndType(
            Doctorant doctorant,
            Publication.TypePublication type
    );
}
