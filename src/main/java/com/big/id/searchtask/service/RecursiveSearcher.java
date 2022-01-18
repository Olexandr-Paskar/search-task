package com.big.id.searchtask.service;

import com.big.id.searchtask.entity.NameOffset;
import com.big.id.searchtask.entity.OffsetDto;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import static com.big.id.searchtask.service.Matcher.SEARCH_RESULT_MAP;
import static com.big.id.searchtask.service.Matcher.recursiveSearcherThreshold;

public class RecursiveSearcher extends RecursiveAction {

    private final List<String> names;
    private final transient List<OffsetDto> offsetDtos;

    public RecursiveSearcher(@NonNull final List<String> names,
                             @NonNull final List<OffsetDto> offsetDtos) {
        this.names = names;
        this.offsetDtos = offsetDtos;
    }

    @Override
    protected void compute() {
        if (offsetDtos.size() <= recursiveSearcherThreshold) {
            enrichMap();
        } else {
            final var crt1 = new RecursiveSearcher(names, offsetDtos.subList(0, offsetDtos.size() / 2));
            final var crt2 = new RecursiveSearcher(names, offsetDtos.subList(offsetDtos.size() / 2, offsetDtos.size()));
            invokeAll(crt1, crt2);
        }
    }

    private void enrichMap() {
        offsetDtos.forEach(dto ->
                names.forEach(name -> {
                    final var singleContentLine = dto.getContentValue().toLowerCase();
                    if (singleContentLine.contains(name.toLowerCase())) {
                        var namePositionInContentLine = singleContentLine.indexOf(name.toLowerCase());
                        var charOffset = dto.getContentValuePosition() + namePositionInContentLine;

                        processContent(dto.getLinePosition(), charOffset, namePositionInContentLine, name);

                        final var repeatedNamesAmount = findAmountOfRepeatedNames(singleContentLine, name.toLowerCase());

                        if (repeatedNamesAmount > 1) {
                            processContentWithRepetitiveNames(name, dto, singleContentLine, namePositionInContentLine, repeatedNamesAmount);
                        }
                    }
                }));
    }

    private void processContent(final int lineOffset,
                                final int charOffset,
                                final int namePositionInContentLine,
                                @NonNull final String name) {
        final List<NameOffset> nameOffsets = SEARCH_RESULT_MAP.get(name);

        if (nameOffsets == null) {
            addNewNameOffsetToMap(lineOffset, charOffset, namePositionInContentLine, name);
        } else {
            nameOffsets.add(new NameOffset(lineOffset, charOffset, namePositionInContentLine));
        }
    }

    private void addNewNameOffsetToMap(final int lineOffset,
                                       final int charOffset,
                                       final int namePositionInContentLine,
                                       @NonNull final String name) {
        final var offsetList = new ArrayList<NameOffset>();
        offsetList.add(new NameOffset(lineOffset, charOffset, namePositionInContentLine));
        SEARCH_RESULT_MAP.put(name, offsetList);
    }

    private long findAmountOfRepeatedNames(@NonNull final String singleContentLine,
                                           @NonNull final String name) {
        return Arrays.stream(singleContentLine.split(" ")).filter(s -> s.contains(name)).count();
    }

    private void processContentWithRepetitiveNames(@NonNull final String name, @NonNull final OffsetDto offsetDto,
                                                   @NonNull final String singleContentLine, int namePositionInContentLine,
                                                   final long repeatedNamesAmount) {
        for (int i = 1; i < repeatedNamesAmount; i++) {
            namePositionInContentLine = singleContentLine.indexOf(name.toLowerCase(), namePositionInContentLine + 1);
            var charOffset = offsetDto.getContentValuePosition() + namePositionInContentLine;
            processContent(offsetDto.getLinePosition(), charOffset, namePositionInContentLine, name);
        }
    }
}
