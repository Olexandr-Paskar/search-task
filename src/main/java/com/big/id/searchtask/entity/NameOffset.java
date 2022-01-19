package com.big.id.searchtask.entity;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NameOffset {
    // line position relative to the entire file
    private int lineOffset;
    // name position relative to the entire file
    private int charOffset;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("[lineOffset=")
                .append(lineOffset)
                .append(", charOffset=")
                .append(charOffset)
                .append("]")
                .toString();
    }
}
