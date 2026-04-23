package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Soutenance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ISoutenanceService {
    Soutenance findById(Long id);
    List<Soutenance> findAll();
    Soutenance planifier(Soutenance soutenance);
    Soutenance modifier(Long id, Soutenance soutenance);
    void supprimer(Long id);
    Optional<Soutenance> findByDemandeSoutenance(DemandeSoutenance demande);
    List<Soutenance> findByPeriode(LocalDate debut, LocalDate fin);
    Soutenance autoriser(Long id);
}
