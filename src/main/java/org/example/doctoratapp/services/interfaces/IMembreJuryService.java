package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.MembreJury;

import java.util.List;

public interface IMembreJuryService {

    MembreJury findById(Long id);
    List<MembreJury> findAll();
    MembreJury ajouter(MembreJury membre);
    MembreJury modifier(Long id, MembreJury membre);
    void supprimer(Long id);
    List<MembreJury> findByDemandeSoutenance(DemandeSoutenance demande);
    List<MembreJury> findByRole(MembreJury.RoleJury role);
}
