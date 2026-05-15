package org.example.doctoratapp.dto.dossier;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la soumission d'un dossier via le formulaire web.
 * Contient les fichiers joints sous forme de MultipartFile.
 */
@Data
public class DossierFormDTO {

    @NotNull(message = "Le sujet de thèse est obligatoire")
    @Size(min = 10, message = "Le sujet doit faire au moins 10 caractères")
    private String sujetThese;

    @NotNull(message = "Veuillez sélectionner un directeur")
    private Long directeurId;

    // Documents obligatoires
    private MultipartFile diplome;
    private MultipartFile cv;
    private MultipartFile lettreMotivation;

    // Documents optionnels
    private MultipartFile[] autresDocuments;
}
