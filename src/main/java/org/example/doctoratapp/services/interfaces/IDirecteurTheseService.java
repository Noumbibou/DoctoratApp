package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DirecteurThese;

import java.util.List;

public interface IDirecteurTheseService {
    DirecteurThese findById(Long id);
    List<DirecteurThese> findAll();
    DirecteurThese ajouter(DirecteurThese directeur);
    DirecteurThese modifier(Long id, DirecteurThese directeur);
    void supprimer(Long id);
    List<DirecteurThese> findByLaboratoire(String laboratoire);
}
