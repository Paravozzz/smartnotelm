package com.smartnotelm.api.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteDetailDto(
    UUID id,
    String title,
    String body,
    UUID groupId,
    String groupName,
    List<UUID> tagIds,
    List<String> tagNames,
    Instant createdAt,
    Instant updatedAt,
    Instant dueAt) {}
