package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ─── Document.java ───────────────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument typeDocument;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false)
    private String cheminFichier;

    @Column(nullable = false)
    private String format; // PDF, JPG, etc.

    @Column(nullable = false)
    private LocalDateTime dateDepot = LocalDateTime.now();

    // ManyToOne : rattaché à un dossier d'inscription (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_inscription_id")
    private DossierInscription dossierInscription;

    // ManyToOne : rattaché à une demande de soutenance (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_soutenance_id")
    private DemandeSoutenance demandeSoutenance;

    // OneToOne : rattaché à une formation doctorale (nullable)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    private FormationDoctorale formation;

    public enum TypeDocument {
        DIPLOME,
        CV,
        LETTRE_MOTIVATION,
        RAPPORT_THESE,
        RAPPORT_ANTIPLAGIAT,
        ATTESTATION_FORMATION,
        RAPPORT_PUBLICATIONS,
        AUTORISATION_SOUTENANCE,
        DEMANDE_MANUSCRITE
    }
}
