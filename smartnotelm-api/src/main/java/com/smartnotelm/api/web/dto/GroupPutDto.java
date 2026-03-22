package com.smartnotelm.api.web.dto;

import java.util.UUID;

public record GroupPutDto(String name, UUID parentId) {}
