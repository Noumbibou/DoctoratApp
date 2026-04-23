package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.FormationDoctorale;

import java.util.List;

public interface IFormationDoctoraleService {

    FormationDoctorale findById(Long id);
    List<FormationDoctorale> findAll();
    FormationDoctorale ajouter(FormationDoctorale formation);
    FormationDoctorale modifier(Long id, FormationDoctorale formation);
    void supprimer(Long id);
    List<FormationDoctorale> findByDoctorant(Doctorant doctorant);
    int getTotalHeures(Doctorant doctorant);
    boolean prerequisFormationRemplis(Doctorant doctorant);
}
