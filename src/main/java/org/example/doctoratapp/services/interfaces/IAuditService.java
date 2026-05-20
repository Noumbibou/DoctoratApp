package org.example.doctoratapp.services.interfaces;

import org.example.doctoratapp.entities.AuditEntry;

public interface IAuditService {
    AuditEntry record(String entityType, Long entityId, String action, String details);
}
