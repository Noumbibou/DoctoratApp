package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ─── DossierInscription.java ─────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DossierInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sujetThese;

    @Column(nullable = false)
    private LocalDate dateDepot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier statut = StatutDossier.SOUMIS;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ManyToOne : plusieurs dossiers pour un doctorant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorant_id", nullable = false)
    private Doctorant doctorant;

    // ManyToOne : plusieurs dossiers encadrés par un directeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directeur_id", nullable = false)
    private DirecteurThese directeurThese;

    // ManyToOne : plusieurs dossiers dans une campagne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campagne_id", nullable = false)
    private CampagneInscription campagne;

    // OneToMany : un dossier a plusieurs documents
    @OneToMany(mappedBy = "dossierInscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();

    public enum StatutDossier {
        SOUMIS,
        EN_ATTENTE_DIRECTEUR,
        EN_ATTENTE_ADMIN,
        VALIDE,
        REJETE
    }
}
