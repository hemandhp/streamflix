package com.streamflix.api.controller;

import com.streamflix.api.dto.CatalogStatus;
import com.streamflix.api.dto.CategorySummary;
import com.streamflix.api.dto.ChannelPageResponse;
import com.streamflix.api.model.Channel;
import com.streamflix.api.service.PlaylistCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final PlaylistCatalogService catalogService;

    public ChannelController(PlaylistCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ChannelPageResponse getChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return catalogService.getAllPaged(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Channel> getChannel(@PathVariable String id) {
        return catalogService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/categories")
    public List<CategorySummary> getCategories() {
        return catalogService.getCategories();
    }

    @GetMapping("/category/{name}")
    public ChannelPageResponse getByCategory(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return catalogService.getByCategory(name, page, size);
    }

    @GetMapping("/search")
    public ChannelPageResponse search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return catalogService.search(q, page, size);
    }

    @GetMapping("/status")
    public CatalogStatus getStatus() {
        return catalogService.getStatus();
    }

    @PostMapping("/refresh")
    public ResponseEntity<CatalogStatus> refresh() {
        catalogService.refresh();
        return ResponseEntity.ok(catalogService.getStatus());
    }
}
