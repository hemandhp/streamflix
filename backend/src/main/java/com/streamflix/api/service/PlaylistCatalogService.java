package com.streamflix.api.service;

import com.streamflix.api.dto.CatalogStatus;
import com.streamflix.api.dto.CategorySummary;
import com.streamflix.api.dto.ChannelPageResponse;
import com.streamflix.api.model.Channel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Holds the in-memory channel catalog built from the configured M3U playlist and
 * refreshes it on a fixed schedule. A failed refresh never wipes a previously good
 * catalog - it just logs and keeps serving the last known-good snapshot.
 */
@Service
public class PlaylistCatalogService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistCatalogService.class);

    private final WebClient webClient;
    private final M3uPlaylistParser parser;
    private final String playlistUrl;

    private final AtomicReference<List<Channel>> catalog = new AtomicReference<>(List.of());
    private volatile Instant lastRefreshedAt;
    private volatile String lastError;

    public PlaylistCatalogService(
            WebClient webClient,
            M3uPlaylistParser parser,
            @Value("${streamflix.playlist.url}") String playlistUrl) {
        this.webClient = webClient;
        this.parser = parser;
        this.playlistUrl = playlistUrl;
    }

    @PostConstruct
    void init() {
        // Deliberately NOT synchronous: the full playlist is ~8,000+ channels and can take
        // 10-30s to download and parse. Doing that inline here would block Spring Boot's
        // startup (Tomcat won't accept connections until context refresh completes), which
        // makes every frontend request fail with connection-refused/502 during that window.
        // Fetch in the background instead so the API is reachable immediately; getStatus()
        // reports loaded=false until the first refresh finishes.
        Thread loader = new Thread(this::refresh, "playlist-initial-load");
        loader.setDaemon(true);
        loader.start();
    }

    @Scheduled(fixedDelayString = "${streamflix.playlist.refresh-interval-ms}")
    public void scheduledRefresh() {
        refresh();
    }

    public synchronized void refresh() {
        log.info("Refreshing playlist catalog from {}", playlistUrl);
        try {
            String body = webClient.get()
                    .uri(playlistUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(45));

            List<Channel> parsed = parser.parse(body);
            if (parsed.isEmpty()) {
                throw new IllegalStateException("Parsed playlist contained zero channels - refusing to replace existing catalog");
            }

            List<Channel> sorted = parsed.stream()
                    .sorted(Comparator.comparing(Channel::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            catalog.set(sorted);
            lastRefreshedAt = Instant.now();
            lastError = null;
            log.info("Playlist catalog refreshed: {} channels across {} categories",
                    sorted.size(), sorted.stream().map(Channel::groupTitle).distinct().count());
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("Failed to refresh playlist catalog, keeping previous snapshot ({} channels): {}",
                    catalog.get().size(), e.toString());
        }
    }

    public List<Channel> getAll() {
        return catalog.get();
    }

    public java.util.Optional<Channel> findById(String id) {
        return catalog.get().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public List<CategorySummary> getCategories() {
        Map<String, Long> counts = catalog.get().stream()
                .collect(Collectors.groupingBy(Channel::groupTitle, Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new CategorySummary(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategorySummary::channelCount).reversed()
                        .thenComparing(CategorySummary::name))
                .toList();
    }

    public ChannelPageResponse getByCategory(String category, int page, int pageSize) {
        List<Channel> filtered = catalog.get().stream()
                .filter(c -> c.groupTitle().equalsIgnoreCase(category))
                .toList();
        return paginate(filtered, page, pageSize);
    }

    public ChannelPageResponse search(String query, int page, int pageSize) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<Channel> filtered = needle.isEmpty()
                ? catalog.get()
                : catalog.get().stream()
                    .filter(c -> c.name().toLowerCase(Locale.ROOT).contains(needle)
                            || c.groupTitle().toLowerCase(Locale.ROOT).contains(needle)
                            || c.country().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        return paginate(filtered, page, pageSize);
    }

    public ChannelPageResponse getAllPaged(int page, int pageSize) {
        return paginate(catalog.get(), page, pageSize);
    }

    public CatalogStatus getStatus() {
        List<Channel> current = catalog.get();
        long categoryCount = current.stream().map(Channel::groupTitle).distinct().count();
        return new CatalogStatus(!current.isEmpty(), current.size(), categoryCount, lastRefreshedAt, playlistUrl, lastError);
    }

    private ChannelPageResponse paginate(List<Channel> source, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 200);
        int fromIndex = Math.min(safePage * safeSize, source.size());
        int toIndex = Math.min(fromIndex + safeSize, source.size());
        return ChannelPageResponse.of(source.subList(fromIndex, toIndex), safePage, safeSize, source.size());
    }
}
