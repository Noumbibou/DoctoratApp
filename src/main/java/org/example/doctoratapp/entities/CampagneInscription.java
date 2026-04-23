package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ─── CampagneInscription.java ────────────────────────────────
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampagneInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateOuverture;

    @Column(nullable = false)
    private LocalDate dateFermeture;

    @Column(nullable = false)
    private String anneeUniversitaire; // ex: "2024-2025"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCampagne type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCampagne statut = StatutCampagne.OUVERTE;

    @OneToMany(mappedBy = "campagne", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DossierInscription> dossiers = new ArrayList<>();

    public enum TypeCampagne {
        INSCRIPTION, REINSCRIPTION
    }

    public enum StatutCampagne {
        OUVERTE, FERMEE
    }
}
