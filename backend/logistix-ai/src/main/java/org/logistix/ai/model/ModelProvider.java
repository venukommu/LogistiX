package org.logistix.ai.model;

import java.util.concurrent.CompletableFuture;

/**
 * Provider-agnostic SPI for invoking foundation models.
 */
public interface ModelProvider {

    String getProviderId();

    String getDefaultModel();

    AiResponse<String> generate(AiRequest request);

    <T> AiResponse<T> generateStructured(AiRequest request, Class<T> responseType);

    CompletableFuture<AiResponse<String>> generateAsync(AiRequest request);
}
