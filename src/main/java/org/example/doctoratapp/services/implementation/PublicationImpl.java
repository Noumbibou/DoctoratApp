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
        if (publication.getDoctorant() == null || publication.getDoctorant().getStatutDoctorant() != Doctorant.Statut.ACTIF) {
            throw new RuntimeException("Le doctorant doit avoir un statut ACTIF pour soumettre une publication.");
        }
        return publicationRepo.save(publication);
    }

    @Override
    public Publication modifier(Long id, Publication publicationModifiee) {
        Publication existante = findById(id);
        if (existante.getDoctorant() == null || existante.getDoctorant().getStatutDoctorant() != Doctorant.Statut.ACTIF) {
            throw new RuntimeException("Le doctorant doit avoir un statut ACTIF pour modifier cette publication.");
        }
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
    public long countByDoctorantAndTypeAndStatutIn(Doctorant doctorant, Publication.TypePublication type, List<Publication.StatutPublication> statuts) {
        return publicationRepo.countByDoctorantAndTypeAndStatutIn(doctorant, type, statuts);
    }

    @Override
    public boolean prerequisPublicationsRemplis(Doctorant doctorant) {
        List<Publication.StatutPublication> statutsValides = List.of(
                Publication.StatutPublication.ACCEPTE,
                Publication.StatutPublication.PUBLIE
        );

        long journauxQ1 = countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q1, statutsValides);
        long journauxQ2 = countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.JOURNAL_Q2, statutsValides);
        long conferences = countByDoctorantAndTypeAndStatutIn(doctorant, Publication.TypePublication.CONFERENCE, statutsValides);
        return (journauxQ1 + journauxQ2) >= 2 && conferences >= 2;
    }
}
