package org.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    private String action;

    private String details;

    protected AuditLog() {
    }

    public AuditLog(String action, String details) {
        this.action = action;
        this.details = details;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
