package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.Derogation;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.User;

import java.util.List;

public interface IDerogationService {

    Derogation findById(Long id);
    List<Derogation> findAll();
    Derogation ajouter(Derogation derogation);
    Derogation modifier(Long id, Derogation derogation);
    void supprimer(Long id);
    List<Derogation> findByDoctorant(Doctorant doctorant);
    List<Derogation> findByStatut(Derogation.StatutDerogation statut);
    Derogation accorder(Long id, User admin);
    Derogation refuser(Long id, User admin);
}
