package com.smartnotelm.api.search;

import java.util.ArrayList;
import java.util.List;

public final class ChunkSplitter {

  private static final int MAX_LEN = 900;
  private static final int OVERLAP = 120;

  private ChunkSplitter() {}

  public static List<String> split(String text) {
    List<String> out = new ArrayList<>();
    if (text == null || text.isBlank()) {
      out.add("");
      return out;
    }
    String t = text.strip();
    int start = 0;
    while (start < t.length()) {
      int end = Math.min(start + MAX_LEN, t.length());
      out.add(t.substring(start, end));
      if (end >= t.length()) {
        break;
      }
      start = Math.max(end - OVERLAP, start + 1);
    }
    return out;
  }
}
