package com.smartnotelm.api.web;

import com.smartnotelm.api.service.TagService;
import com.smartnotelm.api.web.dto.TagDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

  private final TagService tagService;

  @GetMapping
  public List<TagDto> list() {
    return tagService.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TagDto create(@RequestBody Map<String, String> body) {
    return tagService.create(body.get("name"));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    tagService.delete(id);
  }
}
