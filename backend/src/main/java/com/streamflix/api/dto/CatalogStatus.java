package com.streamflix.api.dto;

import java.time.Instant;

public record CatalogStatus(
        boolean loaded,
        long channelCount,
        long categoryCount,
        Instant lastRefreshedAt,
        String sourceUrl,
        String lastError
) {
}
