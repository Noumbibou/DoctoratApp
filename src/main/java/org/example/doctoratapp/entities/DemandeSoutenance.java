package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ─── DemandeSoutenance.java ──────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeSoutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateDepot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut = StatutDemande.SOUMIS;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ManyToOne : plusieurs demandes pour un doctorant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorant_id", nullable = false)
    private Doctorant doctorant;

    // OneToMany : plusieurs documents requis
    @OneToMany(mappedBy = "demandeSoutenance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();

    // OneToMany : plusieurs membres du jury
    @OneToMany(mappedBy = "demandeSoutenance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MembreJury> membresJury = new ArrayList<>();

    // OneToOne : une demande aboutit à une soutenance
    @OneToOne(mappedBy = "demandeSoutenance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Soutenance soutenance;

    public enum StatutDemande {
        SOUMIS,
        PREREQUIS_VALIDES,
        EN_ATTENTE_JURY,
        EN_ATTENTE_RAPPORTS,
        AUTORISEE,
        PLANIFIEE,
        REJETEE
    }
}
