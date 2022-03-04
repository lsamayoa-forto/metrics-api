package com.forto.metrics.api.suggestions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SuggestRequest {
    @JsonProperty()
    private SuggestQueryType type;
    @JsonProperty("q")
    private String query;
    @JsonProperty("max")
    private int maximum;
}
