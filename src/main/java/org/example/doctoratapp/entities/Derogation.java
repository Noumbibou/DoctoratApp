package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Derogation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false)
    private LocalDate dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDerogation statut = StatutDerogation.EN_ATTENTE;

    // ManyToOne : plusieurs dérogations pour un doctorant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorant_id", nullable = false)
    private Doctorant doctorant;

    // ManyToOne : accordée par un admin (User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User accordeePar;

    public enum StatutDerogation {
        EN_ATTENTE, ACCORDEE, REFUSEE
    }
}
