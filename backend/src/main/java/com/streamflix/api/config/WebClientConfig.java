package com.streamflix.api.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(
            @Value("${streamflix.playlist.connect-timeout-ms:15000}")
            int connectTimeoutMs,

            @Value("${streamflix.playlist.read-timeout-ms:60000}")
            int readTimeoutMs) {

        HttpClient httpClient =
                HttpClient.create()

                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                connectTimeoutMs
                        )

                        .responseTimeout(
                                Duration.ofSeconds(60)
                        )

                        .followRedirect(true)

                        .doOnConnected(connection ->
                                connection.addHandlerLast(
                                        new ReadTimeoutHandler(
                                                readTimeoutMs,
                                                TimeUnit.MILLISECONDS
                                        )
                                )
                        );

        return WebClient.builder()

                .clientConnector(
                        new ReactorClientHttpConnector(
                                httpClient
                        )
                )

                .codecs(configurer ->
                        configurer
                                .defaultCodecs()
                                .maxInMemorySize(
                                        50 * 1024 * 1024
                                )
                )

                .defaultHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) "
                                + "Chrome/131.0.0.0 Safari/537.36"
                )

                .defaultHeader(
                        "Accept",
                        "*/*"
                )

                .build();
    }
}