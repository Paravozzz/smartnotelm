package com.smartnotelm.api.repo;

import com.smartnotelm.api.domain.NoteGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteGroupRepository extends JpaRepository<NoteGroup, UUID> {

  List<NoteGroup> findByParentIdOrderByNameAsc(UUID parentId);

  List<NoteGroup> findByParentIsNullOrderByNameAsc();
}
