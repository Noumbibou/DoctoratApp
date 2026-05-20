package org.example.doctoratapp.dto.campagne;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.doctoratapp.entities.CampagneInscription;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampagneInscriptionDTO {

    private Long id;

    @NotNull(message = "La date d'ouverture est obligatoire")
    private LocalDate dateOuverture;

    @NotNull(message = "La date de fermeture est obligatoire")
    private LocalDate dateFermeture;

    @NotBlank(message = "L'année universitaire est obligatoire")
    private String anneeUniversitaire;

    @NotNull(message = "Le type de campagne est obligatoire")
    private CampagneInscription.TypeCampagne type;

    private CampagneInscription.StatutCampagne statut;
}
