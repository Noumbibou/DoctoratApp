package org.example.doctoratapp.dto.doctorant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// dto/doctorant/DoctorantDTO.java
// Représente un doctorant avec ses infos essentielles
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorantDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String numInscription;
    private LocalDate dateInscriptionInitiale;
    private Integer anneeEnCours;
    private String statut;

    // Infos calculées utiles pour l'affichage
    private long anneeDoctorat;      // nombre d'années depuis inscription
    private boolean enDepassement;   // true si > 6 ans
    private boolean peutSeReinscrire; // true si <= 3 ans
}