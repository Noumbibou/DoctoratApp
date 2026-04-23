package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── MembreJury.java ─────────────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembreJury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String etablissement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleJury role;

    // ManyToOne : plusieurs membres pour une demande de soutenance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_soutenance_id", nullable = false)
    private DemandeSoutenance demandeSoutenance;

    public enum RoleJury {
        RAPPORTEUR, EXAMINATEUR, PRESIDENT
    }
}