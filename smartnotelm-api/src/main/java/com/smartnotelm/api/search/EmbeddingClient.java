package com.smartnotelm.api.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartnotelm.api.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {

  private final AppProperties props;
  private final RestClient.Builder restClientBuilder;

  public float[] embed(String input) {
    if (!props.embeddings().enabled()) {
      return new float[0];
    }
    String provider = props.embeddings().provider();
    if ("openai".equalsIgnoreCase(provider)) {
      return embedOpenAi(input);
    }
    return embedOllama(input);
  }

  private float[] embedOllama(String input) {
    var o = props.embeddings().ollama();
    RestClient client = restClientBuilder.baseUrl(trimSlash(o.baseUrl())).build();
    JsonNode root =
        client
            .post()
            .uri("/api/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("model", o.model(), "prompt", input))
            .retrieve()
            .body(JsonNode.class);
    if (root == null || !root.has("embedding")) {
      throw new IllegalStateException("Ollama embeddings: bad response");
    }
    return toFloatArray(root.get("embedding"));
  }

  private float[] embedOpenAi(String input) {
    var c = props.embeddings().openai();
    if (c.apiKey() == null || c.apiKey().isBlank()) {
      throw new IllegalStateException("OpenAI API key missing");
    }
    RestClient client = restClientBuilder.baseUrl(trimSlash(c.baseUrl())).build();
    JsonNode root =
        client
            .post()
            .uri("/v1/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + c.apiKey())
            .body(Map.of("model", c.model(), "input", input))
            .retrieve()
            .body(JsonNode.class);
    if (root == null || !root.has("data") || root.get("data").isEmpty()) {
      throw new IllegalStateException("OpenAI embeddings: bad response");
    }
    return toFloatArray(root.get("data").get(0).get("embedding"));
  }

  private static String trimSlash(String u) {
    if (u.endsWith("/")) {
      return u.substring(0, u.length() - 1);
    }
    return u;
  }

  private static float[] toFloatArray(JsonNode arr) {
    List<Float> list = new ArrayList<>();
    for (JsonNode n : arr) {
      list.add((float) n.asDouble());
    }
    float[] v = new float[list.size()];
    for (int i = 0; i < list.size(); i++) {
      v[i] = list.get(i);
    }
    return v;
  }
}
