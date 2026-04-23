package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ─── FormationDoctorale.java ─────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormationDoctorale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intitule;

    @Column(nullable = false)
    private Integer heures;

    @Column(nullable = false)
    private LocalDate dateFormation;

    // ManyToOne : plusieurs formations pour un doctorant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorant_id", nullable = false)
    private Doctorant doctorant;

    // OneToOne : une attestation (document) par formation
    @OneToOne(mappedBy = "formation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Document attestation;
}
