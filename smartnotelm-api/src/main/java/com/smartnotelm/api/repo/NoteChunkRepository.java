package com.smartnotelm.api.repo;

import com.smartnotelm.api.domain.NoteChunk;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteChunkRepository extends JpaRepository<NoteChunk, UUID> {

  @Modifying
  @Query("delete from NoteChunk c where c.note.id = :noteId")
  void deleteByNoteId(@Param("noteId") UUID noteId);
}
