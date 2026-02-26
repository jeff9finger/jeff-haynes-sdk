package dev.lotr.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a quote from The One API.
 *
 * <p>Instances are immutable after deserialization. All fields correspond
 * to the API's {@code /quote} endpoint response schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
public final class Quote {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("dialog")
    private String dialog;

    @JsonProperty("movie")
    private String movie;

    @JsonProperty("character")
    private String character;

    // Jackson requires a no-arg constructor
    Quote() {}
}
