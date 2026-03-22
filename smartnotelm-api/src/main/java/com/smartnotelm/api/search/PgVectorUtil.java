package com.smartnotelm.api.search;

public final class PgVectorUtil {

  private PgVectorUtil() {}

  /** Literal for PostgreSQL CAST(x AS vector), e.g. '[0.1,0.2]'. */
  public static String toLiteral(float[] v) {
    if (v == null || v.length == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(Float.toString(v[i]));
    }
    sb.append(']');
    return sb.toString();
  }
}
