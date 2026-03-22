package com.smartnotelm.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteChunk {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "note_id")
  private Note note;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  public NoteChunk(UUID id, Note note, int chunkIndex, String content) {
    this.id = id;
    this.note = note;
    this.chunkIndex = chunkIndex;
    this.content = content;
  }
}
