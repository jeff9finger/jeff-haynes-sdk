package dev.lotr.sdk;

/**
 * Integration tests for MovieResource using Apache HttpClient.
 *
 * <p>Requires: {@code LOTR_API_KEY=your-key mvn verify -Pintegration}
 */
class ApacheMovieResourceIT extends MovieResourceITBase {

    @Override
    OneApiClient createClient(String apiKey) {
        return OneApiClient.builder()
                .apiKey(apiKey)
                .httpClient(new ApacheHttpClient())
                .build();
    }
}