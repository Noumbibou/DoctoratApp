package org.example.doctoratapp.repo;

import org.example.doctoratapp.entities.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepo extends JpaRepository<AuditEntry, Long> {
}
