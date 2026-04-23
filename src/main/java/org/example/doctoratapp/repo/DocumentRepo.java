package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.DemandeSoutenance;
import org.example.doctoratapp.entities.Document;
import org.example.doctoratapp.entities.DossierInscription;
import org.example.doctoratapp.entities.FormationDoctorale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {

    // Documents d'un dossier d'inscription
    List<Document> findByDossierInscription(DossierInscription dossier);

    // Documents d'une demande de soutenance
    List<Document> findByDemandeSoutenance(DemandeSoutenance demande);

    // Documents par type
    List<Document> findByTypeDocument(Document.TypeDocument typeDocument);

    // Document d'une formation
    Optional<Document> findByFormation(FormationDoctorale formation);
}
