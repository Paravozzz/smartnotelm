package com.smartnotelm.api.repo;

import com.smartnotelm.api.domain.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

  Optional<Tag> findByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);
}
