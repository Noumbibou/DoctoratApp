package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.DirecteurThese;
import org.example.doctoratapp.repo.DirecteurTheseRepo;
import org.example.doctoratapp.services.interfaces.IDirecteurTheseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DirecteurTheseImpl implements IDirecteurTheseService {

    private DirecteurTheseRepo directeurRepo;

    public DirecteurTheseImpl(DirecteurTheseRepo directeurTheseRepo) {
        this.directeurRepo = directeurTheseRepo;
    }

    @Override
    public DirecteurThese findById(Long id) {
        return directeurRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Directeur introuvable avec l'id : " + id));
    }

    @Override
    public List<DirecteurThese> findAll() {
        return directeurRepo.findAll();
    }

    @Override
    public DirecteurThese ajouter(DirecteurThese directeur) {
        return directeurRepo.save(directeur);
    }

    @Override
    public DirecteurThese modifier(Long id, DirecteurThese directeurModifie) {
        DirecteurThese existant = findById(id);
        existant.setGrade(directeurModifie.getGrade());
        existant.setLaboratoire(directeurModifie.getLaboratoire());
        existant.setSpecialite(directeurModifie.getSpecialite());
        return directeurRepo.save(existant);
    }

    @Override
    public void supprimer(Long id) {
        if (!directeurRepo.existsById(id)) {
            throw new RuntimeException("Directeur introuvable avec l'id : " + id);
        }
        directeurRepo.deleteById(id);
    }

    @Override
    public List<DirecteurThese> findByLaboratoire(String laboratoire) {
        return directeurRepo.findByLaboratoire(laboratoire);
    }
}
