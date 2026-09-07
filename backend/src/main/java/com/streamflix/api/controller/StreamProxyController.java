package com.streamflix.api.controller;

import com.streamflix.api.service.StreamProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StreamProxyController {

    private final StreamProxyService proxyService;

    public StreamProxyController(StreamProxyService proxyService) {
        this.proxyService = proxyService;
    }

    /**
     * Streams (and, for .m3u8 manifests, rewrites) a third-party IPTV URL through the backend
     * so the browser player never needs the origin's own CORS support.
     *
     * Example: GET /api/proxy?url=https%3A%2F%2Fexample.com%2Fchannel%2Findex.m3u8
     */
    @GetMapping("/api/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String url) {
        return proxyService.proxy(url);
    }
}
