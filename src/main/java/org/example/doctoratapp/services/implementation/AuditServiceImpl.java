package org.example.doctoratapp.services.implementation;

import org.example.doctoratapp.entities.AuditEntry;
import org.example.doctoratapp.repo.AuditEntryRepo;
import org.example.doctoratapp.services.interfaces.IAuditService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements IAuditService {

    private final AuditEntryRepo repo;

    public AuditServiceImpl(AuditEntryRepo repo) {
        this.repo = repo;
    }

    @Override
    public AuditEntry record(String entityType, Long entityId, String action, String details) {
        AuditEntry e = new AuditEntry(entityType, entityId, action, details, LocalDateTime.now());
        return repo.save(e);
    }
}
