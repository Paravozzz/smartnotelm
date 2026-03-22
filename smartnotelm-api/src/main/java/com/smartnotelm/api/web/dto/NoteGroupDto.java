package com.smartnotelm.api.web.dto;

import java.util.List;
import java.util.UUID;

public record NoteGroupDto(UUID id, UUID parentId, String name, List<NoteGroupDto> children) {}
