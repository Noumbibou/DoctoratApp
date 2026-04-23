package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Document;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.repo.DoctorantRepo;
import org.example.doctoratapp.repo.DocumentRepo;
import org.example.doctoratapp.services.interfaces.IDoctorantService;
import org.example.doctoratapp.services.interfaces.IDocumentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentImpl implements IDocumentService {

    private DocumentRepo documentRepo;

    public DocumentImpl(DocumentRepo documentRepo) {
        this.documentRepo = documentRepo;
    }

    @Override
    public Document findById(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document introuvable avec l'id : " + id));
    }

    @Override
    public List<Document> findAll() {
        return documentRepo.findAll();
    }

    @Override
    public Document ajouter(Document document) {
        // Vérifier le format autorisé
        List<String> formatsAutorises = List.of("PDF", "JPG", "JPEG", "PNG");
        if (!formatsAutorises.contains(document.getFormat().toUpperCase())) {
            throw new RuntimeException(
                    "Format non autorisé : " + document.getFormat() +
                            ". Formats acceptés : " + formatsAutorises
            );
        }
        document.setDateDepot(LocalDateTime.now());
        return documentRepo.save(document);
    }

    @Override
    public void supprimer(Long id) {
        if (!documentRepo.existsById(id)) {
            throw new RuntimeException("Document introuvable avec l'id : " + id);
        }
        documentRepo.deleteById(id);
    }

    @Override
    public List<Document> findByDossierInscription(DossierInscription dossier) {
        return documentRepo.findByDossierInscription(dossier);
    }

    @Override
    public List<Document> findByDemandeSoutenance(DemandeSoutenance demande) {
        return documentRepo.findByDemandeSoutenance(demande);
    }

    @Override
    public List<Document> findByType(Document.TypeDocument type) {
        return documentRepo.findByTypeDocument(type);
    }
}
