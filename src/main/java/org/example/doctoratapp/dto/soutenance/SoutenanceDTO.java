package org.example.doctoratapp.dto.soutenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoutenanceDTO {

    private Long id;

    @NotNull(message = "La date est obligatoire")
    private LocalDate dateSoutenance;

    @NotNull(message = "L'heure est obligatoire")
    private LocalTime heure;

    @NotBlank(message = "Le lieu est obligatoire")
    private String lieu;

    private Boolean autorisationAdmin;
    private String mention;
    private Long demandeSoutenanceId;
    private Long doctorantId;
    private String doctorantNom;
}
