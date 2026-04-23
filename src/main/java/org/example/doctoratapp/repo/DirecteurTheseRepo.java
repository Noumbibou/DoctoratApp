package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.DirecteurThese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirecteurTheseRepo extends JpaRepository<DirecteurThese, Long> {
    // Trouver par laboratoire
    List<DirecteurThese> findByLaboratoire(String laboratoire);

    // Trouver par grade
    List<DirecteurThese> findByGrade(String grade);
}
