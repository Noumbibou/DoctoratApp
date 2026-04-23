package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.Doctorant;
import org.example.doctoratapp.entities.Publication;
import org.example.doctoratapp.repo.PublicationRepo;
import org.example.doctoratapp.services.interfaces.IPublicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicationImpl implements IPublicationService {

    private PublicationRepo publicationRepo;

    public PublicationImpl(PublicationRepo publicationRepo) {
        this.publicationRepo = publicationRepo;
    }

    @Override
    public Publication findById(Long id) {
        return publicationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication introuvable avec l'id : " + id));
    }

    @Override
    public List<Publication> findAll() {
        return publicationRepo.findAll();
    }

    @Override
    public Publication ajouter(Publication publication) {
        return publicationRepo.save(publication);
    }

    @Override
    public Publication modifier(Long id, Publication publicationModifiee) {
        Publication existante = findById(id);
        existante.setTitre(publicationModifiee.getTitre());
        existante.setType(publicationModifiee.getType());
        existante.setRevue(publicationModifiee.getRevue());
        existante.setAnnee(publicationModifiee.getAnnee());
        existante.setStatut(publicationModifiee.getStatut());
        return publicationRepo.save(existante);
    }

    @Override
    public void supprimer(Long id) {
        if (!publicationRepo.existsById(id)) {
            throw new RuntimeException("Publication introuvable avec l'id : " + id);
        }
        publicationRepo.deleteById(id);
    }

    @Override
    public List<Publication> findByDoctorant(Doctorant doctorant) {
        return publicationRepo.findByDoctorant(doctorant);
    }

    @Override
    public long countByDoctorantAndType(Doctorant doctorant, Publication.TypePublication type) {
        return publicationRepo.countByDoctorantAndType(doctorant, type);
    }

    @Override
    public boolean prerequisPublicationsRemplis(Doctorant doctorant) {
        long journauxQ1 = countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q1);
        long journauxQ2 = countByDoctorantAndType(doctorant, Publication.TypePublication.JOURNAL_Q2);
        long conferences = countByDoctorantAndType(doctorant, Publication.TypePublication.CONFERENCE);
        return (journauxQ1 + journauxQ2) >= 2 && conferences >= 2;
    }
}
