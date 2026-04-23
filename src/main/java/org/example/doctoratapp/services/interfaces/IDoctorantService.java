package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.Doctorant;

import java.util.List;
import java.util.Optional;

public interface IDoctorantService {
    Doctorant findById(Long id);
    List<Doctorant> findAll();
    Doctorant ajouter(Doctorant doctorant);
    Doctorant modifier(Long id, Doctorant doctorant);
    void supprimer(Long id);
    List<Doctorant> findByStatut(Doctorant.Statut statut);
    Optional<Doctorant> findByNumInscription(String numInscription);
    List<Doctorant> findDoctorantsEnDepassement();
}
