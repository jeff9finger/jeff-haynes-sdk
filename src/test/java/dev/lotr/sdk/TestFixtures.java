package dev.lotr.sdk;

/**
 * Shared JSON fixtures for unit tests.
 *
 * <p>Centralizing test data avoids duplication and makes it easy to keep
 * fixtures consistent with the real API response structure.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static final String MOVIE_LIST_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd95395de30eff6ebccde5c",
                  "name": "The Fellowship of the Ring",
                  "runtimeInMinutes": 178,
                  "budgetInMillions": 93,
                  "boxOfficeRevenueInMillions": 871.5,
                  "academyAwardNominations": 13,
                  "academyAwardWins": 4,
                  "rottenTomatoesScore": 91
                },
               {
                    "_id": "5cd95395de30eff6ebccde5b",
                    "name": "The Two Towers",
                    "runtimeInMinutes": 179,
                    "budgetInMillions": 94,
                    "boxOfficeRevenueInMillions": 926,
                    "academyAwardNominations": 6,
                    "academyAwardWins": 2,
                    "rottenTomatoesScore": 96
                },
                {
                  "_id": "5cd95395de30eff6ebccde5d",
                  "name": "The Return of the King",
                  "runtimeInMinutes": 201,
                  "budgetInMillions": 94,
                  "boxOfficeRevenueInMillions": 1120,
                  "academyAwardNominations": 11,
                  "academyAwardWins": 11,
                  "rottenTomatoesScore": 95
                }
              ],
              "total": 3,
              "limit": 1000,
              "offset": 0,
              "page": 1,
              "pages": 1
            }
            """;

    public static final String SINGLE_MOVIE_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd95395de30eff6ebccde5d",
                  "name": "The Return of the King",
                  "runtimeInMinutes": 201,
                  "budgetInMillions": 94,
                  "boxOfficeRevenueInMillions": 1120,
                  "academyAwardNominations": 11,
                  "academyAwardWins": 11,
                  "rottenTomatoesScore": 95
                }
              ],
              "total": 1,
              "limit": 1000,
              "offset": 0,
              "page": 1,
              "pages": 1
            }
            """;

    public static final String QUOTE_LIST_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd96e05de30eff6ebcce7e9",
                  "dialog": "One Ring to rule them all.",
                  "movie": "5cd95395de30eff6ebccde5d",
                  "character": "5cd99d4bde30eff6ebccfbe6"
                },
                {
                  "_id": "5cd96e05de30eff6ebcce7ea",
                  "dialog": "My precious.",
                  "movie": "5cd95395de30eff6ebccde5d",
                  "character": "5cd99d4bde30eff6ebccfe9e"
                }
              ],
              "total": 2,
              "limit": 1000,
              "offset": 0,
              "page": 1,
              "pages": 1
            }
            """;

    public static final String SINGLE_QUOTE_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd96e05de30eff6ebcce7e9",
                  "dialog": "One Ring to rule them all.",
                  "movie": "5cd95395de30eff6ebccde5d",
                  "character": "5cd99d4bde30eff6ebccfbe6"
                }
              ],
              "total": 1,
              "limit": 1000,
              "offset": 0,
              "page": 1,
              "pages": 1
            }
            """;

    public static final String EMPTY_RESPONSE_JSON = """
            {
              "docs": [],
              "total": 0,
              "limit": 1000,
              "offset": 0,
              "page": 1,
              "pages": 1
            }
            """;

    /** Two-page dataset for pagination testing. */
    public static final String PAGE_1_OF_3_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd95395de30eff6ebccde5c",
                  "name": "The Fellowship of the Ring",
                  "runtimeInMinutes": 178,
                  "budgetInMillions": 93,
                  "boxOfficeRevenueInMillions": 871.5,
                  "academyAwardNominations": 13,
                  "academyAwardWins": 4,
                  "rottenTomatoesScore": 91
                }
              ],
              "total": 3,
              "limit": 1,
              "offset": 0,
              "page": 1,
              "pages": 3
            }
            """;

    public static final String PAGE_2_OF_3_JSON = """
            {
              "docs": [
                {
                    "_id": "5cd95395de30eff6ebccde5b",
                    "name": "The Two Towers",
                    "runtimeInMinutes": 179,
                    "budgetInMillions": 94,
                    "boxOfficeRevenueInMillions": 926,
                    "academyAwardNominations": 6,
                    "academyAwardWins": 2,
                    "rottenTomatoesScore": 96
                }
              ],
              "total": 3,
              "limit": 1,
              "offset": 0,
              "page": 2,
              "pages": 3
            }
            """;

    public static final String PAGE_3_OF_3_JSON = """
            {
              "docs": [
                {
                  "_id": "5cd95395de30eff6ebccde5d",
                  "name": "The Return of the King",
                  "runtimeInMinutes": 201,
                  "budgetInMillions": 94,
                  "boxOfficeRevenueInMillions": 1120,
                  "academyAwardNominations": 11,
                  "academyAwardWins": 11,
                  "rottenTomatoesScore": 95
                }
              ],
              "total": 3,
              "limit": 1,
              "offset": 0,
              "page": 3,
              "pages": 3
            }
            """;
}
