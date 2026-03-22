package com.smartnotelm.api.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class VectorSearchDao {

  @PersistenceContext private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  public List<UUID> nearestNoteIds(String vectorLiteral, int limit) {
    return entityManager
        .createNativeQuery(
            """
            SELECT sub.note_id FROM (
              SELECT nc.note_id AS note_id,
                     MIN(nc.embedding <=> CAST(:vec AS vector)) AS dist
              FROM note_chunks nc
              WHERE nc.embedding IS NOT NULL
              GROUP BY nc.note_id
              ORDER BY dist ASC
              LIMIT :lim
            ) sub
            """)
        .setParameter("vec", vectorLiteral)
        .setParameter("lim", limit)
        .getResultList()
        .stream()
        .map(r -> (UUID) r)
        .toList();
  }
}
