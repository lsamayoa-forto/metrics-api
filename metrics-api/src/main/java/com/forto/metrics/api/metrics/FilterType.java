package com.forto.metrics.api.metrics;

import lombok.Getter;

public enum FilterType {

    ILiteralOr("iliteral_or"), // one or more values, case insensitive (a, b, c) if series contains any of these
    WildCard("wildcard"), // a wildcard, case sensitive
    NotLiteralOr("not_literal_or"), // series contains none of these
    NotILiteralOr("not_iliteral_or"), // series contains none of these, case insensitive
    NotKey("not_key"), // skips any time series with the supplied key
    IWildcard("iwildcard"), // a wildcard, case sensitive
    LiteralOr("literal_or"), // one or more values, case sensitive, (a, b, c) if series contains any of these
    RegularExpression("regexp"); // regular expression to evaluate.

    @Getter
    private final String jsonName;

    FilterType(String jsonName) {
        this.jsonName = jsonName;
    }
}
