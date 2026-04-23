package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.DossierInscription;

import java.util.List;

public interface IDossierInscriptionService {

    DossierInscription findById(Long id);
    List<DossierInscription> findAll();
    DossierInscription ajouter(DossierInscription dossier);
    DossierInscription modifier(Long id, DossierInscription dossier);
    void supprimer(Long id);
    List<DossierInscription> findByDoctorant(Doctorant doctorant);
    List<DossierInscription> findByStatut(DossierInscription.StatutDossier statut);
    List<DossierInscription> findByDirecteur(DirecteurThese directeur);
    DossierInscription changerStatut(Long id, DossierInscription.StatutDossier statut);
}
