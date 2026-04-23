package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.FormationDoctorale;
import org.example.doctoratapp.repo.FormationDoctoraleRepo;
import org.example.doctoratapp.services.interfaces.IFormationDoctoraleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FormationDoctoraleImpl implements IFormationDoctoraleService {

    private FormationDoctoraleRepo formationRepo;

    public FormationDoctoraleImpl(FormationDoctoraleRepo formationRepo) {
        this.formationRepo = formationRepo;
    }

    @Override
    public FormationDoctorale findById(Long id) {
        return formationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation introuvable avec l'id : " + id));
    }

    @Override
    public List<FormationDoctorale> findAll() {
        return formationRepo.findAll();
    }

    @Override
    public FormationDoctorale ajouter(FormationDoctorale formation) {
        return formationRepo.save(formation);
    }

    @Override
    public FormationDoctorale modifier(Long id, FormationDoctorale formationModifiee) {
        FormationDoctorale existante = findById(id);
        existante.setIntitule(formationModifiee.getIntitule());
        existante.setHeures(formationModifiee.getHeures());
        existante.setDateFormation(formationModifiee.getDateFormation());
        return formationRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!formationRepo.existsById(id)) {
            throw new RuntimeException("Formation introuvable avec l'id : " + id);
        }
        formationRepo.deleteById(id);
    }

    @Override
    public List<FormationDoctorale> findByDoctorant(Doctorant doctorant) {
        return formationRepo.findByDoctorant(doctorant);
    }

    @Override
    public int getTotalHeures(Doctorant doctorant) {
        return formationRepo.sumHeuresByDoctorant(doctorant);
    }

    @Override
    public boolean prerequisFormationRemplis(Doctorant doctorant) {
        return getTotalHeures(doctorant) >= 200;
    }
}
