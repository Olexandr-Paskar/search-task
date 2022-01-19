package com.big.id.searchtask.service;

import com.big.id.searchtask.entity.NameOffset;
import com.big.id.searchtask.entity.OffsetDto;
import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

@AllArgsConstructor
public class RecursiveSearcher extends RecursiveAction {
    private final int threshold;
    private final List<String> names;
    private final transient ConcurrentMap<String, List<NameOffset>> searchResultMap;
    private final transient List<OffsetDto> offsetDtos;

    @Override
    protected void compute() {
        if (offsetDtos.size() <= threshold) {
            enrichMap();
        } else {
            final var crt1 = new RecursiveSearcher(threshold, names, searchResultMap, offsetDtos.subList(0, offsetDtos.size() / 2));
            final var crt2 = new RecursiveSearcher(threshold, names, searchResultMap, offsetDtos.subList(offsetDtos.size() / 2, offsetDtos.size()));
            invokeAll(crt1, crt2);
        }
    }

    private void enrichMap() {
        offsetDtos.forEach(dto ->
                names.forEach(name -> {
                    final var singleContentLine = dto.getContentValue().toLowerCase();
                    final var namePositionInContentLine = singleContentLine.indexOf(name.toLowerCase());
                    if (namePositionInContentLine != -1) {
                        final var repeatedNamesPosition = findRepeatedNamesPosition(singleContentLine, name.toLowerCase());
                        if (repeatedNamesPosition.size() > 1) {
                            processContentWithRepetitiveNames(name, dto, repeatedNamesPosition, singleContentLine);
                        } else if (isNameMatches(singleContentLine, name)) {
                            processContentWithSingleNameInIt(name, dto, namePositionInContentLine);
                        }
                    }
                }));
    }

    private void processContentWithSingleNameInIt(@NonNull final String name,
                                                  @NonNull final OffsetDto offsetDto,
                                                  final int namePositionInContentLine) {
        final List<NameOffset> nameOffsets = searchResultMap.get(name);
        final int charOffset = offsetDto.getContentValuePosition() + namePositionInContentLine;

        if (nameOffsets == null) {
            addNewNameOffsetToMap(offsetDto.getLinePosition(), charOffset, name);
        } else {
            nameOffsets.add(new NameOffset(offsetDto.getLinePosition(), charOffset));
        }
    }

    private void addNewNameOffsetToMap(final int lineOffset,
                                       final int charOffset,
                                       @NonNull final String name) {
        final var offsetList = new ArrayList<NameOffset>();
        offsetList.add(new NameOffset(lineOffset, charOffset));
        searchResultMap.put(name, offsetList);
    }

    @NonNull
    private List<Integer> findRepeatedNamesPosition(@NonNull final String singleContentLine,
                                                    @NonNull final String name) {
        final var indexes = new ArrayList<Integer>();
        var nameLength = 0;
        var index = 0;

        while (index != -1) {
            index = singleContentLine.indexOf(name, index + nameLength);
            if (index != -1) {
                indexes.add(index);
            }
            nameLength = name.length();
        }
        return indexes;
    }

    private boolean isNameMatches(@NonNull final String singleContentLine,
                                  @NonNull final String name) {
        return Pattern.compile(".*\\b" + name.toLowerCase() + "\\b.*").matcher(singleContentLine).matches();
    }

    private void processContentWithRepetitiveNames(@NonNull final String name,
                                                   @NonNull final OffsetDto offsetDto,
                                                   @NonNull final List<Integer> repeatedNamesPosition,
                                                   @NonNull final String singleContentLine) {
        repeatedNamesPosition
                .stream()
                .filter(position -> getStringWhichContainsName(name, singleContentLine, position)
                        .replaceAll("[^a-zA-Z0-9]+", "").equalsIgnoreCase(name))
                .forEach(index -> processContentWithSingleNameInIt(name, offsetDto, index));
    }

    @NonNull
    private String getStringWhichContainsName(@NonNull final String name,
                                              @NonNull final String singleContentLine,
                                              @NonNull final Integer positionOfWordWhichContainsName) {
        final String subStringWhichContainsName;
        if (positionOfWordWhichContainsName == 0) {
            subStringWhichContainsName = singleContentLine.substring(positionOfWordWhichContainsName, positionOfWordWhichContainsName + name.length());
        } else {
            final var substring = singleContentLine.substring(positionOfWordWhichContainsName - 1, positionOfWordWhichContainsName + name.length());
            if (singleContentLine.endsWith(substring)) {
                subStringWhichContainsName = substring;
            } else {
                subStringWhichContainsName = singleContentLine.substring(positionOfWordWhichContainsName - 1, positionOfWordWhichContainsName + name.length() + 1);
            }
        }
        return subStringWhichContainsName;
    }
}
