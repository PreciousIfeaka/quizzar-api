package com.quizzar.generation.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class GeminiWebClientConfig {

    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient(GeminiProperties props) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectSeconds() * 1000)
            .responseTimeout(Duration.ofSeconds(props.getTimeout().getReadSeconds()))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(props.getTimeout().getReadSeconds(), TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(props.getTimeout().getReadSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Gemini Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Gemini Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
