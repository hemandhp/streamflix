package com.streamflix.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StreamProxyService {

    private static final Pattern URI_ATTRIBUTE =
            Pattern.compile("URI=\"([^\"]+)\"");

    private final WebClient webClient;
    private final SsrfGuard ssrfGuard;

    public StreamProxyService(
            WebClient webClient,
            SsrfGuard ssrfGuard,
            @Value("${streamflix.proxy.max-redirects:5}") int maxRedirects) {

        this.webClient = webClient;
        this.ssrfGuard = ssrfGuard;
    }

    public ResponseEntity<byte[]> proxy(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            URI target = URI.create(rawUrl);

            ssrfGuard.assertSafeToFetch(target);

            ResponseEntity<byte[]> upstream = webClient
                    .get()
                    .uri(target)
                    .header(
                            HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                    + "AppleWebKit/537.36 "
                                    + "(KHTML, like Gecko) "
                                    + "Chrome/131.0.0.0 Safari/537.36"
                    )
                    .header(HttpHeaders.ACCEPT, "*/*")
                    .header(HttpHeaders.REFERER, target.getScheme() + "://" + target.getHost() + "/")
                    .header(HttpHeaders.CONNECTION, "keep-alive")
                    .exchangeToMono(response ->
                            response.toEntity(byte[].class)
                    )
                    .block(Duration.ofSeconds(30));

            if (upstream == null) {
                return ResponseEntity
                        .status(502)
                        .body("Upstream returned no response"
                                .getBytes(StandardCharsets.UTF_8));
            }

            HttpStatusCode status = upstream.getStatusCode();

            byte[] body = upstream.getBody() == null
                    ? new byte[0]
                    : upstream.getBody();

            System.out.println(
                    "Proxy: " + target +
                    " -> " + status +
                    " (" + body.length + " bytes)"
            );

            if (!status.is2xxSuccessful()) {
                return ResponseEntity
                        .status(502)
                        .body(
                                ("Upstream returned HTTP " + status.value())
                                        .getBytes(StandardCharsets.UTF_8)
                        );
            }

            MediaType contentType =
                    upstream.getHeaders().getContentType();

            if (isManifest(rawUrl, contentType)) {

                String manifest =
                        new String(body, StandardCharsets.UTF_8);

                String rewritten =
                        rewriteManifest(manifest, target);

                return ResponseEntity.ok()
                        .contentType(
                                MediaType.parseMediaType(
                                        "application/vnd.apple.mpegurl"
                                )
                        )
                        .header(
                                HttpHeaders.CACHE_CONTROL,
                                "no-store, no-cache, must-revalidate"
                        )
                        .body(
                                rewritten.getBytes(StandardCharsets.UTF_8)
                        );
            }

            MediaType effectiveType =
                    contentType != null
                            ? contentType
                            : guessContentType(rawUrl);

            return ResponseEntity.ok()
                    .contentType(effectiveType)
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store"
                    )
                    .body(body);

        } catch (Exception e) {

            System.err.println(
                    "HLS PROXY ERROR: " + rawUrl
            );

            e.printStackTrace();

            return ResponseEntity
                    .status(502)
                    .body(
                            ("Proxy error: " + e.getMessage())
                                    .getBytes(StandardCharsets.UTF_8)
                    );
        }
    }

    private boolean isManifest(
            String url,
            MediaType contentType) {

        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains(".m3u8")) {
            return true;
        }

        if (contentType == null) {
            return false;
        }

        String type =
                contentType.toString().toLowerCase();

        return type.contains("mpegurl")
                || type.contains("vnd.apple.mpegurl");
    }

    private String rewriteManifest(
            String manifestText,
            URI baseUri) {

        StringBuilder output =
                new StringBuilder();

        String[] lines =
                manifestText.split("\\r?\\n", -1);

        for (String line : lines) {

            String trimmed =
                    line.trim();

            if (trimmed.isEmpty()) {
                output.append("\n");
                continue;
            }

            if (trimmed.startsWith("#")) {

                output.append(
                        rewriteTagAttributes(
                                trimmed,
                                baseUri
                        )
                );

            } else {

                String resolved =
                        resolve(baseUri, trimmed);

                output.append(
                        toProxyPath(resolved)
                );
            }

            output.append("\n");
        }

        return output.toString();
    }

    private String rewriteTagAttributes(
            String tagLine,
            URI baseUri) {

        Matcher matcher =
                URI_ATTRIBUTE.matcher(tagLine);

        StringBuffer result =
                new StringBuffer();

        while (matcher.find()) {

            String original =
                    matcher.group(1);

            String resolved =
                    resolve(baseUri, original);

            String proxied =
                    toProxyPath(resolved);

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            "URI=\"" + proxied + "\""
                    )
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String resolve(
            URI baseUri,
            String reference) {

        try {
            return baseUri
                    .resolve(reference)
                    .toString();

        } catch (Exception e) {
            return reference;
        }
    }

    private String toProxyPath(
            String absoluteTargetUrl) {

        return UriComponentsBuilder
                .fromPath("/api/proxy")
                .queryParam(
                        "url",
                        absoluteTargetUrl
                )
                .build()
                .encode()
                .toUriString();
    }

    private MediaType guessContentType(
            String url) {

        String lower =
                url.toLowerCase();

        if (lower.contains(".ts")) {
            return MediaType.valueOf(
                    "video/mp2t"
            );
        }

        if (lower.contains(".m4s")) {
            return MediaType.valueOf(
                    "video/iso.segment"
            );
        }

        if (lower.contains(".mp4")) {
            return MediaType.valueOf(
                    "video/mp4"
            );
        }

        if (lower.contains(".aac")) {
            return MediaType.valueOf(
                    "audio/aac"
            );
        }

        if (lower.contains(".key")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}