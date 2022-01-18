package com.big.id.searchtask.service;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class Reader {
    @Value("${norvig.url}")
    private String norvigUrl;

    private final WebClient webClient;

    public Reader(WebClient webClient) {
        this.webClient = webClient;
    }

    @SneakyThrows
    public String getContent() {
        return Files.readString(Path.of("norvigText.txt"));
//        The code below is needed if we do not have a file itself
//        In order to make GET call and get data we use WebClient

//        return webClient
//                .get()
//                .uri(norvigUrl)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
    }
}
