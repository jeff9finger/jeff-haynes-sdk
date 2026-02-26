package dev.lotr.sdk;

/**
 * Integration tests for MovieResource using the default Java HTTP client.
 *
 * <p>Requires: {@code LOTR_API_KEY=your-key mvn verify -Pintegration}
 */
class MovieResourceIT extends MovieResourceITBase {

    @Override
    OneApiClient createClient(String apiKey) {
        return OneApiClient.builder()
                .apiKey(apiKey)
                .maxRetries(3)
                .build();
    }
}