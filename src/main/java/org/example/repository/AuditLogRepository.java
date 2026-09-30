package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public AuditLog save(AuditLog auditLog) {
        entityManager.persist(auditLog);
        return auditLog;
    }

    public List<AuditLog> findAll() {
        return entityManager.createQuery("select a from AuditLog a order by a.id", AuditLog.class)
                .getResultList();
    }
}
