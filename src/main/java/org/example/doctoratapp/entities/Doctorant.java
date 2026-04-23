package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Doctorant extends User{

    @Column(unique = true, nullable = false)
    private String numInscription;

    @Column(nullable = false)
    private LocalDate dateInscriptionInitiale;

    private Integer anneeEnCours;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Statut statutDoctorant = Statut.ACTIF;

    @OneToMany(mappedBy = "doctorant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DossierInscription> dossiers = new ArrayList<>();

    @OneToMany(mappedBy = "doctorant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Publication> publications = new ArrayList<>();

    @OneToMany(mappedBy = "doctorant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FormationDoctorale> formations = new ArrayList<>();

    @OneToMany(mappedBy = "doctorant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DemandeSoutenance> demandesSoutenance = new ArrayList<>();

    @OneToMany(mappedBy = "doctorant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Derogation> derogations = new ArrayList<>();

    public enum Statut{
        ACTIF, SUSPENDU, DIPLOME
    }
}
