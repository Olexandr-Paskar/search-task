package com.big.id.searchtask.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class Aggregator {
    private final Matcher matcher;

    public void aggregateResult() {
        matcher.getSearchResultMap()
                .forEach((firstName, offsetList) ->
                        System.out.println(new StringBuilder()
                                .append(firstName)
                                .append(" -->")
                                .append(offsetList)));
    }
}
