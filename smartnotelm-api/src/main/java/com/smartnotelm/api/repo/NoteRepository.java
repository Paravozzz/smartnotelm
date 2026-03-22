package com.smartnotelm.api.repo;

import com.smartnotelm.api.domain.Note;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID> {

  List<Note> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

  @Query(
      """
      SELECT DISTINCT n FROM Note n
      LEFT JOIN FETCH n.tags
      LEFT JOIN FETCH n.group
      """)
  List<Note> findAllForList();

  @Query(
      """
      SELECT DISTINCT n FROM Note n
      LEFT JOIN FETCH n.tags
      LEFT JOIN FETCH n.group
      WHERE n.id = :id
      """)
  java.util.Optional<Note> findDetailedById(@Param("id") UUID id);

  @Query(
      value =
          """
          SELECT n.id, ts_rank(n.search_vector, plainto_tsquery('simple', :q)) AS rank
          FROM notes n
          WHERE n.search_vector @@ plainto_tsquery('simple', :q)
          ORDER BY rank DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Object[]> searchIdsByFts(@Param("q") String q, @Param("limit") int limit);
}
