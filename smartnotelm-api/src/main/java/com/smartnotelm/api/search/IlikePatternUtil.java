package com.smartnotelm.api.search;

/** Escaping for PostgreSQL {@code ILIKE ... ESCAPE '\\'}. */
public final class IlikePatternUtil {

  private IlikePatternUtil() {}

  public static String escape(String raw) {
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /** Pattern {@code %...%} over the trimmed substring; use with {@code ESCAPE '\\'}. */
  public static String substringPattern(String raw) {
    return "%" + escape(raw.trim()) + "%";
  }
}
