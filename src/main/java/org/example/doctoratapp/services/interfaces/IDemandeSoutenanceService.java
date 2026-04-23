package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Publication;
import org.example.doctoratapp.entities.Soutenance;

import java.util.List;

public interface IDemandeSoutenanceService {
    boolean isPrerequísRemplis(DemandeSoutenance demandeSoutenance, List<Publication> publications, int totalHeureFormattion);

    DemandeSoutenance findById(Long id);
    List<DemandeSoutenance> findAll();
    DemandeSoutenance ajouter(DemandeSoutenance demande);
    DemandeSoutenance modifier(Long id, DemandeSoutenance demande);
    void supprimer(Long id);
    List<DemandeSoutenance> findByDoctorant(Doctorant doctorant);
    List<DemandeSoutenance> findByStatut(DemandeSoutenance.StatutDemande statut);
    DemandeSoutenance changerStatut(Long id, DemandeSoutenance.StatutDemande statut);
    boolean verifierTousPrerequis(Doctorant doctorant);
    List<String> getPrerequísNonRemplis(Doctorant doctorant);
}
