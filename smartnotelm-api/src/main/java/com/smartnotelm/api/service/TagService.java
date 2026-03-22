package com.smartnotelm.api.service;

import com.smartnotelm.api.domain.Tag;
import com.smartnotelm.api.repo.TagRepository;
import com.smartnotelm.api.web.dto.TagDto;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TagService {

  private final TagRepository tagRepository;

  public List<TagDto> list() {
    return tagRepository.findAll().stream()
        .sorted(Comparator.comparing(Tag::getName))
        .map(t -> new TagDto(t.getId(), t.getName(), t.isSystem()))
        .toList();
  }

  @Transactional
  public TagDto create(String name) {
    if (tagRepository.existsByNameIgnoreCase(name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag exists");
    }
    Tag t = new Tag(UUID.randomUUID(), name, false);
    t = tagRepository.save(t);
    return new TagDto(t.getId(), t.getName(), t.isSystem());
  }

  @Transactional
  public void delete(UUID id) {
    Tag t =
        tagRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag"));
    if (t.isSystem()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System tag");
    }
    tagRepository.delete(t);
  }
}
