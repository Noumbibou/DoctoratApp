package org.example.doctoratapp.dto.document;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private Long id;

    @NotNull(message = "Le type de document est obligatoire")
    private String typeDocument;

    private String nomFichier;
    private String format;
    private LocalDateTime dateDepot;

    // Référence au dossier ou à la soutenance (nullable)
    private Long dossierInscriptionId;
    private Long demandeSoutenanceId;
    private Long formationId;
}
