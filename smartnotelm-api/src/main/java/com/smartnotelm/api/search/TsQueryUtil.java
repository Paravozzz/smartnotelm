package com.smartnotelm.api.search;

import java.util.ArrayList;
import java.util.List;

/** Builds PostgreSQL {@code to_tsquery('simple', ...)} strings with prefix matching per token. */
public final class TsQueryUtil {

  private TsQueryUtil() {}

  /**
   * Each whitespace-separated token becomes {@code token:*} (prefix match); tokens are AND-ed.
   * Non-letter/digit/underscore characters are stripped from each token. Returns "" if nothing
   * usable remains — caller should skip FTS.
   */
  public static String prefixTsQuery(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    List<String> clauses = new ArrayList<>();
    for (String part : raw.trim().split("\\s+")) {
      if (part.isEmpty()) {
        continue;
      }
      String token = part.replaceAll("[^\\p{L}\\p{N}_]+", "");
      if (token.isEmpty()) {
        continue;
      }
      clauses.add(token + ":*");
    }
    return String.join(" & ", clauses);
  }
}
