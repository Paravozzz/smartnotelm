package com.smartnotelm.api.web.dto;

import java.util.UUID;

public record TagDto(UUID id, String name, boolean system) {}
