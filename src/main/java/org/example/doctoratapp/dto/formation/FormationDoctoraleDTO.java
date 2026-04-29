package org.example.doctoratapp.dto.formation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormationDoctoraleDTO {

    private Long id;

    @NotBlank(message = "L'intitulé est obligatoire")
    private String intitule;

    @NotNull(message = "Le nombre d'heures est obligatoire")
    @Min(value = 1, message = "Le nombre d'heures doit être supérieur à 0")
    private Integer heures;

    @NotNull(message = "La date est obligatoire")
    private LocalDate dateFormation;

    private Long doctorantId;
    private Long attestationId; // id du document attestation
}
