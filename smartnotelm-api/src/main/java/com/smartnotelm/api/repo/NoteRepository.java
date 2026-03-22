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
          SELECT n.id, ts_rank(n.search_vector, to_tsquery('simple', :tsQuery)) AS rank
          FROM notes n
          WHERE n.search_vector @@ to_tsquery('simple', :tsQuery)
          ORDER BY rank DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Object[]> searchIdsByFts(@Param("tsQuery") String tsQuery, @Param("limit") int limit);

  @Query(
      value =
          """
          SELECT n.id FROM notes n
          WHERE n.title ILIKE :pattern ESCAPE '\\' OR n.body ILIKE :pattern ESCAPE '\\'
          ORDER BY n.updated_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<UUID> searchIdsByIlike(@Param("pattern") String pattern, @Param("limit") int limit);
}
