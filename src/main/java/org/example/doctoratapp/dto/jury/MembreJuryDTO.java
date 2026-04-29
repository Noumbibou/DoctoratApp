package org.example.doctoratapp.dto.jury;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembreJuryDTO {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le grade est obligatoire")
    private String grade;

    @NotBlank(message = "L'établissement est obligatoire")
    private String etablissement;

    @NotNull(message = "Le rôle est obligatoire")
    private String role; // RAPPORTEUR, EXAMINATEUR, PRESIDENT

    private Long demandeSoutenanceId;
}
