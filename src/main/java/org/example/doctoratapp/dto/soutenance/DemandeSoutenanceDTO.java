package org.example.doctoratapp.dto.soutenance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeSoutenanceDTO {

    private Long id;
    private LocalDate dateDepot;
    private String statut;
    private String commentaire;
    private Long doctorantId;
    private String doctorantNom;
    private String doctorantPrenom;

    // Résumé des prérequis pour affichage dashboard
    private boolean publicationsOk;
    private boolean formationOk;
    private List<String> prerequisManquants;
}
