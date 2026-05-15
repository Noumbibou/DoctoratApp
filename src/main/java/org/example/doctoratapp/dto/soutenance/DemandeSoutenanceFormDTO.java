package org.example.doctoratapp.dto.soutenance;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO pour le formulaire de demande de soutenance.
 * Contient les fichiers PDF obligatoires.
 */
@Data
public class DemandeSoutenanceFormDTO {

    private MultipartFile demandeManuscrite;
    private MultipartFile rapportThese;
    private MultipartFile rapportAntiPlagiat;
    private MultipartFile rapportPublications;
    private MultipartFile attestationsFormations;
    private MultipartFile autorisationSoutenance;
}
