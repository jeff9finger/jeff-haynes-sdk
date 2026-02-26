package dev.lotr.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a movie from The One API.
 *
 * <p>Instances are immutable after deserialization. All fields correspond
 * to the API's {@code /movie} endpoint response schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
public final class Movie {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("runtimeInMinutes")
    private int runtimeInMinutes;

    @JsonProperty("budgetInMillions")
    private double budgetInMillions;

    @JsonProperty("boxOfficeRevenueInMillions")
    private double boxOfficeRevenueInMillions;

    @JsonProperty("academyAwardNominations")
    private int academyAwardNominations;

    @JsonProperty("academyAwardWins")
    private int academyAwardWins;

    @JsonProperty("rottenTomatoesScore")
    private double rottenTomatoesScore;

    // Jackson requires a no-arg constructor
    Movie() {}
}
