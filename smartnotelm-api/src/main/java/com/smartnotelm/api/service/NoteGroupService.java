package com.smartnotelm.api.service;

import com.smartnotelm.api.domain.NoteGroup;
import com.smartnotelm.api.repo.NoteGroupRepository;
import com.smartnotelm.api.web.dto.NoteGroupDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NoteGroupService {

  private final NoteGroupRepository groupRepository;

  public List<NoteGroupDto> tree() {
    List<NoteGroup> all = groupRepository.findAll();
    Map<UUID, NoteGroupDto> dtos = new HashMap<>();
    for (NoteGroup g : all) {
      dtos.put(g.getId(), new NoteGroupDto(g.getId(), parentId(g), g.getName(), new ArrayList<>()));
    }
    List<NoteGroupDto> roots = new ArrayList<>();
    for (NoteGroup g : all) {
      NoteGroupDto dto = dtos.get(g.getId());
      if (g.getParent() == null) {
        roots.add(dto);
      } else {
        NoteGroupDto parentDto = dtos.get(g.getParent().getId());
        if (parentDto != null) {
          parentDto.children().add(dto);
        }
      }
    }
    roots.sort(java.util.Comparator.comparing(NoteGroupDto::name));
    sortChildren(roots);
    return roots;
  }

  private void sortChildren(List<NoteGroupDto> nodes) {
    for (NoteGroupDto n : nodes) {
      n.children().sort(java.util.Comparator.comparing(NoteGroupDto::name));
      sortChildren(n.children());
    }
  }

  private static UUID parentId(NoteGroup g) {
    return g.getParent() != null ? g.getParent().getId() : null;
  }

  @Transactional
  public NoteGroupDto create(UUID parentId, String name) {
    NoteGroup parent = null;
    if (parentId != null) {
      parent =
          groupRepository
              .findById(parentId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent group"));
    }
    NoteGroup g = new NoteGroup(UUID.randomUUID(), parent, name);
    g = groupRepository.save(g);
    return new NoteGroupDto(g.getId(), parentId, g.getName(), List.of());
  }

  @Transactional
  public NoteGroupDto update(UUID id, String name, UUID parentId) {
    NoteGroup g =
        groupRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group"));
    if (parentId != null) {
      if (parentId.equals(id)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group cannot be parent of itself");
      }
      if (isDescendant(id, parentId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cycle in group hierarchy");
      }
      NoteGroup parent =
          groupRepository
              .findById(parentId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent"));
      g.setParent(parent);
    } else {
      g.setParent(null);
    }
    g.setName(name);
    g = groupRepository.save(g);
    return new NoteGroupDto(g.getId(), g.getParent() != null ? g.getParent().getId() : null, g.getName(), List.of());
  }

  private boolean isDescendant(UUID ancestorId, UUID candidateParentId) {
    UUID current = candidateParentId;
    int guard = 0;
    while (current != null && guard++ < 256) {
      if (current.equals(ancestorId)) {
        return true;
      }
      NoteGroup step = groupRepository.findById(current).orElse(null);
      current = step != null && step.getParent() != null ? step.getParent().getId() : null;
    }
    return false;
  }

  @Transactional
  public void delete(UUID id) {
    if (!groupRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group");
    }
    groupRepository.deleteById(id);
  }
}
