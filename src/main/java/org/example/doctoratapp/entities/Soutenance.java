package org.example.doctoratapp.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

// ─── Soutenance.java ─────────────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Soutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateSoutenance;

    @Column(nullable = false)
    private LocalTime heure;

    @Column(nullable = false)
    private String lieu;

    private Boolean autorisationAdmin = false;

    private String mention; // ex: Très Honorable

    // OneToOne : une soutenance est liée à une demande
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_soutenance_id", nullable = false)
    private DemandeSoutenance demandeSoutenance;
}
