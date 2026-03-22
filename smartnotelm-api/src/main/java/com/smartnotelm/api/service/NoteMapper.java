package com.smartnotelm.api.service;

import com.smartnotelm.api.domain.Note;
import com.smartnotelm.api.domain.Tag;
import com.smartnotelm.api.web.dto.NoteDetailDto;
import com.smartnotelm.api.web.dto.NoteSummaryDto;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class NoteMapper {

  private static final int PREVIEW_MAX = 200;

  private NoteMapper() {}

  public static String bodyPreview(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    String oneLine = body.replaceAll("\\s+", " ").strip();
    if (oneLine.length() <= PREVIEW_MAX) {
      return oneLine;
    }
    return oneLine.substring(0, PREVIEW_MAX);
  }

  public static NoteSummaryDto toSummary(Note n) {
    var tags = n.getTags().stream().sorted(Comparator.comparing(Tag::getName)).toList();
    return new NoteSummaryDto(
        n.getId(),
        n.getTitle(),
        bodyPreview(n.getBody()),
        n.getGroup() != null ? n.getGroup().getId() : null,
        n.getGroup() != null ? n.getGroup().getName() : null,
        tags.stream().map(Tag::getId).collect(Collectors.toList()),
        tags.stream().map(Tag::getName).collect(Collectors.toList()),
        n.getCreatedAt(),
        n.getDueAt());
  }

  public static NoteDetailDto toDetail(Note n) {
    var tags = n.getTags().stream().sorted(Comparator.comparing(Tag::getName)).toList();
    return new NoteDetailDto(
        n.getId(),
        n.getTitle(),
        n.getBody(),
        n.getGroup() != null ? n.getGroup().getId() : null,
        n.getGroup() != null ? n.getGroup().getName() : null,
        tags.stream().map(Tag::getId).collect(Collectors.toList()),
        tags.stream().map(Tag::getName).collect(Collectors.toList()),
        n.getCreatedAt(),
        n.getUpdatedAt(),
        n.getDueAt());
  }
}
