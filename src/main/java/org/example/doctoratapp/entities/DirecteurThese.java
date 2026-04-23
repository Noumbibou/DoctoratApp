package org.example.doctoratapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirecteurThese extends User {

    @Column(nullable = false)
    private String grade;

    private String laboratoire;

    private String specialite;

    @OneToMany(mappedBy = "directeurThese", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DossierInscription> dossiersSuivis = new ArrayList<>();
}
