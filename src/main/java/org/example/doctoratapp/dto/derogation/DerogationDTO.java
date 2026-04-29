package org.example.doctoratapp.dto.derogation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DerogationDTO {

    private Long id;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;

    private LocalDate dateDemande;
    private String statut; // EN_ATTENTE, ACCORDEE, REFUSEE

    private Long doctorantId;
    private String doctorantNom;
    private String doctorantPrenom;

    private Long adminId; // id de l'admin qui a accordé/refusé
    private String adminNom;
}
