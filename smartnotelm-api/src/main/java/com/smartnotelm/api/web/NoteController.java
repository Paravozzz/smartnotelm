package com.smartnotelm.api.web;

import com.smartnotelm.api.service.NoteService;
import com.smartnotelm.api.web.dto.NoteDetailDto;
import com.smartnotelm.api.web.dto.NoteSummaryDto;
import com.smartnotelm.api.web.dto.NoteWriteDto;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

  private final NoteService noteService;

  @GetMapping
  public List<NoteSummaryDto> list(
      @RequestParam(defaultValue = "flat") String view,
      @RequestParam(defaultValue = "created_at") String sort,
      @RequestParam(defaultValue = "desc") String order,
      @RequestParam(required = false) UUID groupId,
      @RequestParam(required = false) UUID tagId) {
    return noteService.list(view, sort, order, groupId, tagId);
  }

  @GetMapping("/{id}")
  public NoteDetailDto get(@PathVariable UUID id) {
    return noteService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NoteDetailDto create(@Valid @RequestBody NoteWriteDto dto) {
    return noteService.create(dto);
  }

  @PutMapping("/{id}")
  public NoteDetailDto update(@PathVariable UUID id, @Valid @RequestBody NoteWriteDto dto) {
    return noteService.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    noteService.delete(id);
  }
}
