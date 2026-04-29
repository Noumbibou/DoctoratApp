package org.example.doctoratapp.dto.publication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationDTO {

    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotNull(message = "Le type est obligatoire")
    private String type; // JOURNAL_Q1, JOURNAL_Q2, CONFERENCE

    private String revue;
    private Integer annee;
    private String url;
    private String statut;
    private Long doctorantId;
}
