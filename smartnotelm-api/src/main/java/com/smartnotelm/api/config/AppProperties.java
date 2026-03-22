package com.smartnotelm.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Cors cors,
    Embeddings embeddings
) {
  public record Cors(String[] allowedOrigins) {}

  public record Embeddings(
      boolean enabled,
      int dimensions,
      String provider,
      Ollama ollama,
      OpenAi openai
  ) {
    public record Ollama(String baseUrl, String model) {}

    public record OpenAi(String baseUrl, String apiKey, String model) {}
  }
}
