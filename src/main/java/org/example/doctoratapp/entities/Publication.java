package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── Publication.java ────────────────────────────────────────
@Entity
@Table(name = "publications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePublication type;

    private String revue;

    private Integer annee;

    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPublication statut = StatutPublication.SOUMIS;

    // ManyToOne : plusieurs publications pour un doctorant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorant_id", nullable = false)
    private Doctorant doctorant;

    public enum TypePublication {
        JOURNAL_Q1,
        JOURNAL_Q2,
        CONFERENCE
    }

    public enum StatutPublication {
        SOUMIS, ACCEPTE, PUBLIE
    }
}
