package com.smartnotelm.api.web;

import com.smartnotelm.api.service.NoteGroupService;
import com.smartnotelm.api.web.dto.GroupPutDto;
import com.smartnotelm.api.web.dto.NoteGroupDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class NoteGroupController {

  private final NoteGroupService groupService;

  @GetMapping("/tree")
  public List<NoteGroupDto> tree() {
    return groupService.tree();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteGroupDto create(@RequestBody Map<String, Object> body) {
    UUID parentId = body.get("parentId") != null ? UUID.fromString(body.get("parentId").toString()) : null;
    String name = body.get("name").toString();
    return groupService.create(parentId, name);
  }

  @PutMapping("/{id}")
  public NoteGroupDto put(@PathVariable UUID id, @RequestBody GroupPutDto dto) {
    return groupService.update(id, dto.name(), dto.parentId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    groupService.delete(id);
  }
}
