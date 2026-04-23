package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Document;
import org.example.doctoratapp.entities.DossierInscription;

import java.util.List;

public interface IDocumentService {
    Document findById(Long id);
    List<Document> findAll();
    Document ajouter(Document document);
    void supprimer(Long id);
    List<Document> findByDossierInscription(DossierInscription dossier);
    List<Document> findByDemandeSoutenance(DemandeSoutenance demande);
    List<Document> findByType(Document.TypeDocument type);
}
