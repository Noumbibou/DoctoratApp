package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.MembreJury;
import org.example.doctoratapp.repo.MembreJuryRepo;
import org.example.doctoratapp.services.interfaces.IMembreJuryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MembreJuryImpl implements IMembreJuryService {

    private MembreJuryRepo membreJuryRepo;

    public MembreJuryImpl(MembreJuryRepo membreJuryRepo) {
        this.membreJuryRepo = membreJuryRepo;
    }

    @Override
    public MembreJury findById(Long id) {
        return membreJuryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Membre jury introuvable avec l'id : " + id));
    }

    @Override
    public List<MembreJury> findAll() {
        return membreJuryRepo.findAll();
    }

    @Override
    public MembreJury ajouter(MembreJury membre) {
        // Vérifier qu'il y a au plus 1 président par demande
        if (membre.getRole() == MembreJury.RoleJury.PRESIDENT) {
            List<MembreJury> presidents = membreJuryRepo
                    .findByDemandeSoutenanceAndRole(
                            membre.getDemandeSoutenance(),
                            MembreJury.RoleJury.PRESIDENT
                    );
            if (!presidents.isEmpty()) {
                throw new RuntimeException("Un président du jury existe déjà pour cette soutenance");
            }
        }
        return membreJuryRepo.save(membre);
    }

    @Override
    public MembreJury modifier(Long id, MembreJury membreModifie) {
        MembreJury existant = findById(id);
        existant.setNom(membreModifie.getNom());
        existant.setPrenom(membreModifie.getPrenom());
        existant.setGrade(membreModifie.getGrade());
        existant.setEtablissement(membreModifie.getEtablissement());
        existant.setRole(membreModifie.getRole());
        return membreJuryRepo.save(existant);
    }

    @Override
    public void supprimer(Long id) {
        if (!membreJuryRepo.existsById(id)) {
            throw new RuntimeException("Membre jury introuvable avec l'id : " + id);
        }
        membreJuryRepo.deleteById(id);
    }

    @Override
    public List<MembreJury> findByDemandeSoutenance(DemandeSoutenance demande) {
        return membreJuryRepo.findByDemandeSoutenance(demande);
    }

    @Override
    public List<MembreJury> findByRole(MembreJury.RoleJury role) {
        return membreJuryRepo.findByDemandeSoutenanceAndRole(null, role);
    }
}
