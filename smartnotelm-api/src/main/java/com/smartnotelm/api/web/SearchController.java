package com.smartnotelm.api.web;

import com.smartnotelm.api.service.SearchService;
import com.smartnotelm.api.web.dto.SearchResultDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;

  @GetMapping
  public List<SearchResultDto> search(
      @RequestParam String q, @RequestParam(defaultValue = "both") String mode) {
    return searchService.search(q, mode);
  }
}
