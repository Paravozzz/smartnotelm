package com.smartnotelm.api.web.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteWriteDto(
    @Size(max = 2048) String title,
    String body,
    UUID groupId,
    List<UUID> tagIds,
    Instant dueAt) {}
