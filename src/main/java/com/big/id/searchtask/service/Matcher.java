package com.big.id.searchtask.service;

import com.big.id.searchtask.entity.NameOffset;
import com.big.id.searchtask.entity.OffsetDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;

@Service
public class Matcher {
    @Value("#{'${names}'.split(',')}")
    private List<String> names;

    private final Reader reader;

    public Matcher(Reader reader) {
        this.reader = reader;
    }

    @NonNull
    public ConcurrentMap<String, List<NameOffset>> getSearchResultMap() {
        final var splitContent = reader.getContent().split("\n");
        final var filteredContent = new ArrayList<OffsetDto>(splitContent.length);
        var generalContentValuePosition = 0;

        for (int i = 0; i < splitContent.length; i++) {
            final String contentValue = splitContent[i];
            if (contentValue.equals("")) {
                continue;
            }
            filteredContent.add(new OffsetDto()
                    .setContentValue(contentValue)
                    .setLinePosition(i + 1)
                    .setContentValuePosition(generalContentValuePosition));
            generalContentValuePosition += contentValue.length();
        }

        final var searchResultMap = new ConcurrentHashMap<String, List<NameOffset>>(names.size());
        final var recursiveSearcherThreshold = getRecursiveSearchThreshold(filteredContent.size());

        ForkJoinPool
                .commonPool()
                .invoke(new RecursiveSearcher(recursiveSearcherThreshold, names, searchResultMap, filteredContent));
        return searchResultMap;
    }

    /**
     * In order to calculate threshold we use next formula ->
     * T = N / (C * L)
     * Where T - threshold, N - size of the task, C - available amount of CPUs,
     * L - load factor, determines how many tasks each worker will perform
     *
     * @param taskSize - the size of the task
     * @return threshold
     */
    public int getRecursiveSearchThreshold(final int taskSize) {
        // provides "reserve" of tasks that amortize delays and unevenness
        final var thresholdLoadFactor = 100;
        return taskSize / (Runtime.getRuntime().availableProcessors() * thresholdLoadFactor);
    }
}