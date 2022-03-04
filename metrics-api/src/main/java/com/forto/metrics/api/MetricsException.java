package com.forto.metrics.api;

import lombok.Getter;

public class MetricsException extends Exception {

    /*
     * List of codes that exceptions refer to.
     * 1: Failed to read from getTags future. Tags are asynchronously fetched from C*, this op failed.
     * 2: Failed to read from getSuggestions future. Suggestions are asynchronously fetched from C*, this op failed.
     * 3: Failed to read from getSeries future. Series are asynchronously fetched from C*, this op failed.
     */
    @Getter
    private final int number;

    public MetricsException(int number, String message, Exception cause) {
        super(message, cause);
        this.number = number;
    }

    public MetricsException(int number, String message) {
        this(number, message, null);
    }

}
