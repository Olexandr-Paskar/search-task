package com.big.id.searchtask;

import com.big.id.searchtask.service.Aggregator;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@AllArgsConstructor
public class SearchTaskApplication implements CommandLineRunner {

    private final Aggregator aggregator;

    public static void main(String[] args) {
        SpringApplication.run(SearchTaskApplication.class, args);
    }

    @Override
    public void run(String... args) {
        aggregator.aggregateResult();
    }
}
