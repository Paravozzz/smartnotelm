package com.smartnotelm.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "note_groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteGroup {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private NoteGroup parent;

  @OneToMany(mappedBy = "parent")
  @Setter(AccessLevel.NONE)
  private List<NoteGroup> children = new ArrayList<>();

  @Column(nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false)
  @Setter(AccessLevel.NONE)
  private Instant createdAt = Instant.now();

  public NoteGroup(UUID id, NoteGroup parent, String name) {
    this.id = id;
    this.parent = parent;
    this.name = name;
  }
}
