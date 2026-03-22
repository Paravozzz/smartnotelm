package com.smartnotelm.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private NoteGroup group;

  @Column(nullable = false)
  private String title = "";

  @Column(nullable = false, columnDefinition = "text")
  private String body = "";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "due_at")
  private Instant dueAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "note_tags",
      joinColumns = @JoinColumn(name = "note_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  @Setter(AccessLevel.NONE)
  private Set<Tag> tags = new HashSet<>();

  public Note(UUID id, NoteGroup group, String title, String body) {
    this.id = id;
    this.group = group;
    this.title = title;
    this.body = body;
  }
}
