package com.forto.metrics.api.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RateOptions {

    private boolean counter;
    private int counterMax;
    private int resetValue;
    private boolean dropResets;
}
