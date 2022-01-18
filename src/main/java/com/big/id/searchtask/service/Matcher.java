package com.big.id.searchtask.service;

import com.big.id.searchtask.entity.NameOffset;
import com.big.id.searchtask.entity.OffsetDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

@Service
public class Matcher {

    private static final String LINE_BRAKE = "\n";
    // provides "reserve" of tasks that amortize delays and unevenness
    private static final int THRESHOLD_LOAD_FACTOR = 100;
    private int generalContentValuePosition;

    @Value("#{'${names}'.split(',')}")
    private List<String> names;
    protected static final ConcurrentMap<String, List<NameOffset>> SEARCH_RESULT_MAP = new ConcurrentHashMap<>();
    protected static int recursiveSearcherThreshold;

    private final Reader reader;

    public Matcher(Reader reader) {
        this.reader = reader;
    }

    public ConcurrentMap<String, List<NameOffset>> getSearchResultMap() {
        final var splitContent = reader.getContent().split(LINE_BRAKE);
        final var offsetDtos = new ArrayList<OffsetDto>(splitContent.length);

        IntStream.range(0, splitContent.length).forEachOrdered(i -> {
            final var contentValue = splitContent[i];
            offsetDtos.add(new OffsetDto()
                    .setContentValue(contentValue)
                    .setLinePosition(i + 1)
                    .setContentValuePosition(generalContentValuePosition));
            generalContentValuePosition += contentValue.chars().count();
        });
        final var filteredContent = offsetDtos
                .parallelStream()
                .filter(offsetDto -> !offsetDto.getContentValue().equals(""))
                .toList();

        recursiveSearcherThreshold = setRecursiveSearchThreshold(filteredContent.size());

        ForkJoinPool
                .commonPool()
                .invoke(new RecursiveSearcher(names, filteredContent));
        return SEARCH_RESULT_MAP;
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
    public int setRecursiveSearchThreshold(final int taskSize) {
        return taskSize / (Runtime.getRuntime().availableProcessors() * THRESHOLD_LOAD_FACTOR);
    }
}

