package com.smartnotelm.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private boolean system;

  @ManyToMany(mappedBy = "tags")
  @Setter(AccessLevel.NONE)
  private Set<Note> notes = new HashSet<>();

  public Tag(UUID id, String name, boolean system) {
    this.id = id;
    this.name = name;
    this.system = system;
  }
}
