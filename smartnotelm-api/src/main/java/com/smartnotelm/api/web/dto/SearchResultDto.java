package com.smartnotelm.api.web.dto;

import java.util.UUID;

public record SearchResultDto(UUID noteId, String title, String bodyPreview, String matchType, double score) {}
