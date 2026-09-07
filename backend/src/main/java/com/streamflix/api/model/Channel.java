package com.streamflix.api.model;

/**
 * A single channel entry parsed out of an M3U/M3U8 playlist.
 * Immutable - a fresh playlist refresh produces a fresh set of these rather than mutating existing ones.
 */
public record Channel(
        String id,
        String tvgId,
        String name,
        String logo,
        String groupTitle,
        String country,
        String language,
        String streamUrl
) {

    public Channel {
        // Normalize nulls to empty strings so the frontend never has to null-check every field.
        name = name == null ? "Untitled channel" : name;
        logo = logo == null ? "" : logo;
        groupTitle = (groupTitle == null || groupTitle.isBlank()) ? "General" : groupTitle;
        country = country == null ? "" : country;
        language = language == null ? "" : language;
    }
}
