package com.streamflix.api.service;

import com.streamflix.api.model.Channel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the extended-M3U format used by iptv-org, e.g.:
 *
 * <pre>
 * #EXTM3U
 * #EXTINF:-1 tvg-id="CNNInternational.us" tvg-country="US" tvg-language="English"
 *   tvg-logo="https://.../cnn.png" group-title="News",CNN International
 * https://example.com/cnn/index.m3u8
 * </pre>
 *
 * The parser is intentionally forgiving: playlists are community-maintained and lines
 * are sometimes malformed, missing attributes, or contain stray VLC/EXTGRP directives.
 * Anything it can't make sense of is skipped rather than aborting the whole parse.
 */
@Component
public class M3uPlaylistParser {

    private static final Pattern EXTINF_PATTERN =
            Pattern.compile("^#EXTINF:(-?\\d+)\\s*(.*?),(.*)$");
    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("([a-zA-Z0-9-]+)=\"([^\"]*)\"");

    public List<Channel> parse(String playlistText) {
        List<Channel> channels = new ArrayList<>();
        if (playlistText == null || playlistText.isBlank()) {
            return channels;
        }

        String[] lines = playlistText.split("\r?\n");

        Map<String, String> pendingAttrs = null;
        String pendingName = null;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#EXTM3U")) {
                continue;
            }

            if (line.startsWith("#EXTINF")) {
                Matcher m = EXTINF_PATTERN.matcher(line);
                if (m.matches()) {
                    pendingAttrs = parseAttributes(m.group(2));
                    pendingName = m.group(3).strip();
                } else {
                    // Malformed EXTINF line - drop whatever channel we were about to build.
                    pendingAttrs = null;
                    pendingName = null;
                }
                continue;
            }

            if (line.startsWith("#")) {
                // #EXTGRP, #EXTVLCOPT, #KODIPROP, comments etc. - not needed for playback.
                continue;
            }

            // Anything else is treated as the stream URI belonging to the preceding #EXTINF.
            if (pendingName != null && looksLikeUrl(line)) {
                channels.add(toChannel(pendingAttrs, pendingName, line));
            }
            pendingAttrs = null;
            pendingName = null;
        }

        return channels;
    }

    private boolean looksLikeUrl(String line) {
        return line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtmp");
    }

    private Map<String, String> parseAttributes(String attrSegment) {
        Map<String, String> attrs = new LinkedHashMap<>();
        if (attrSegment == null) {
            return attrs;
        }
        Matcher m = ATTRIBUTE_PATTERN.matcher(attrSegment);
        while (m.find()) {
            attrs.put(m.group(1).toLowerCase(), m.group(2));
        }
        return attrs;
    }

    private Channel toChannel(Map<String, String> attrs, String name, String streamUrl) {
        if (attrs == null) {
            attrs = Map.of();
        }
        String tvgId = attrs.getOrDefault("tvg-id", "");
        String logo = attrs.getOrDefault("tvg-logo", "");
        String group = attrs.getOrDefault("group-title", "General");
        String country = attrs.getOrDefault("tvg-country", inferCountryFromTvgId(tvgId));
        String language = attrs.getOrDefault("tvg-language", "");

        String id = Integer.toHexString((tvgId + "|" + streamUrl).hashCode());

        return new Channel(id, tvgId, name, logo, group, country, language, streamUrl);
    }

    /** iptv-org tvg-ids are usually "ChannelName.countrycode", e.g. "CNN.us". */
    private String inferCountryFromTvgId(String tvgId) {
        if (tvgId == null || !tvgId.contains(".")) {
            return "";
        }
        String suffix = tvgId.substring(tvgId.lastIndexOf('.') + 1);
        return suffix.length() == 2 ? suffix.toUpperCase() : "";
    }
}
