package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.repo.DoctorantRepo;
import org.example.doctoratapp.services.interfaces.IDoctorantService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorantImpl implements IDoctorantService {

    private DoctorantRepo doctorantRepo;

    public DoctorantImpl(DoctorantRepo doctorantRepo) {
        this.doctorantRepo = doctorantRepo;
    }

    @Override
    public Doctorant findById(Long id) {
        return doctorantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctorant introuvable avec l'id : " + id));
    }

    @Override
    public List<Doctorant> findAll() {
        return doctorantRepo.findAll();
    }

    @Override
    public Doctorant ajouter(Doctorant doctorant) {
        return doctorantRepo.save(doctorant);
    }

    @Override
    public Doctorant modifier(Long id, Doctorant doctorantModifie) {
        Doctorant existant = findById(id);
        existant.setNumInscription(doctorantModifie.getNumInscription());
        existant.setAnneeEnCours(doctorantModifie.getAnneeEnCours());
        existant.setStatutDoctorant(doctorantModifie.getStatutDoctorant());
        return doctorantRepo.save(existant);
    }

    @Override
    public void supprimer(Long id) {
        if (!doctorantRepo.existsById(id)) {
            throw new RuntimeException("Doctorant introuvable avec l'id : " + id);
        }
        doctorantRepo.deleteById(id);
    }

    @Override
    public List<Doctorant> findByStatut(Doctorant.Statut statut) {
        return doctorantRepo.findByStatutDoctorant(statut);
    }

    @Override
    public Optional<Doctorant> findByNumInscription(String numInscription) {
        return doctorantRepo.findByNumInscription(numInscription);
    }

    @Override
    public List<Doctorant> findDoctorantsEnDepassement() {
        // Tous les doctorants inscrits depuis plus de 6 ans
        LocalDate limiteDate = LocalDate.now().minusYears(6);
        return doctorantRepo.findByDateInscriptionInitialeBefore(limiteDate);
    }
//    public DoctorantImpl(DoctorantRepo doctorantRepo) {
//        this.doctorantRepo = doctorantRepo;
//    }
//
//    @Override
//    public boolean isDepassementMaximal(Doctorant doctorant) {
//        if (doctorant.getDateInscriptionInitiale() == null) return false;
//        return ChronoUnit.YEARS.between(doctorant.getDateInscriptionInitiale(), LocalDate.now()) >= 6;
//    }
//
//    @Override
//    public boolean isReInscriptionAutorisee(Doctorant doctorant) {
//        if (doctorant.getDateInscriptionInitiale() == null) return false;
//        return ChronoUnit.YEARS.between(doctorant.getDateInscriptionInitiale(), LocalDate.now()) <= 3;
//    }
}
