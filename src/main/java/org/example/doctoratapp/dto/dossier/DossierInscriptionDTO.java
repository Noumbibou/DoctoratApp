package org.example.doctoratapp.dto.dossier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DossierInscriptionDTO {

    private Long id;

    @NotBlank(message = "Le sujet de thèse est obligatoire")
    private String sujetThese;

    private LocalDate dateDepot;
    private String statut;
    private String commentaire;

    // On retourne juste les ids et noms, pas les objets complets
    // Evite les boucles infinies JSON
    private Long doctorantId;
    private String doctorantNom;
    private String doctorantPrenom;

    private Long directeurId;
    private String directeurNom;
    private String directeurPrenom;

    private Long campagneId;
    private String anneeUniversitaire;
}
