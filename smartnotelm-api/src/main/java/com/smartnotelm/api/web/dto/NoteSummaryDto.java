package com.smartnotelm.api.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteSummaryDto(
    UUID id,
    String title,
    String bodyPreview,
    UUID groupId,
    String groupName,
    List<UUID> tagIds,
    List<String> tagNames,
    Instant createdAt,
    Instant dueAt) {}
