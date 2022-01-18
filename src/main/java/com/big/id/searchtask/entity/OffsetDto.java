package com.big.id.searchtask.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class OffsetDto {
    private String contentValue;
    private int linePosition;
    private int contentValuePosition;
}
