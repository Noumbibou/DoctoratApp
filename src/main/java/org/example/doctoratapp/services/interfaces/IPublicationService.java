package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Publication;

import java.util.List;

public interface IPublicationService {

    Publication findById(Long id);
    List<Publication> findAll();
    Publication ajouter(Publication publication);
    Publication modifier(Long id, Publication publication);
    void supprimer(Long id);
    List<Publication> findByDoctorant(Doctorant doctorant);
    long countByDoctorantAndType(Doctorant doctorant, Publication.TypePublication type);
    boolean prerequisPublicationsRemplis(Doctorant doctorant);
}
