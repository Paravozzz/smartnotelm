package com.smartnotelm.api.service;

import com.smartnotelm.api.config.AppProperties;
import com.smartnotelm.api.domain.Note;
import com.smartnotelm.api.repo.NoteRepository;
import com.smartnotelm.api.search.EmbeddingClient;
import com.smartnotelm.api.search.PgVectorUtil;
import com.smartnotelm.api.search.VectorSearchDao;
import com.smartnotelm.api.web.dto.SearchResultDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

  private static final int RRF_K = 60;
  private static final int LIMIT = 30;

  private final NoteRepository noteRepository;
  private final VectorSearchDao vectorSearchDao;
  private final EmbeddingClient embeddingClient;
  private final AppProperties appProperties;

  @Transactional(readOnly = true)
  public List<SearchResultDto> search(String q, String mode) {
    if (q == null || q.isBlank()) {
      return List.of();
    }
    String m = mode == null ? "both" : mode.toLowerCase();
    List<UUID> fts = List.of();
    List<UUID> vec = List.of();
    if ("text".equals(m) || "both".equals(m)) {
      fts = ftsIds(q);
    }
    if (("semantic".equals(m) || "both".equals(m)) && appProperties.embeddings().enabled()) {
      vec = vectorIds(q);
    }
    if ("both".equals(m) && !fts.isEmpty() && !vec.isEmpty()) {
      return mergeRrf(fts, vec);
    }
    if ("semantic".equals(m) || ("both".equals(m) && fts.isEmpty())) {
      return toResults(vec, "semantic");
    }
    return toResults(fts, "text");
  }

  private List<UUID> ftsIds(String q) {
    List<Object[]> rows = noteRepository.searchIdsByFts(q.trim(), LIMIT);
    List<UUID> ids = new ArrayList<>();
    for (Object[] row : rows) {
      ids.add((UUID) row[0]);
    }
    return ids;
  }

  private List<UUID> vectorIds(String q) {
    try {
      float[] v = embeddingClient.embed(q);
      if (v.length == 0) {
        return List.of();
      }
      return vectorSearchDao.nearestNoteIds(PgVectorUtil.toLiteral(v), LIMIT);
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<SearchResultDto> mergeRrf(List<UUID> fts, List<UUID> vec) {
    Map<UUID, Double> score = new HashMap<>();
    for (int i = 0; i < fts.size(); i++) {
      UUID id = fts.get(i);
      score.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
    }
    for (int i = 0; i < vec.size(); i++) {
      UUID id = vec.get(i);
      score.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
    }
    Set<UUID> order = new LinkedHashSet<>();
    order.addAll(fts);
    order.addAll(vec);
    List<UUID> sorted =
        order.stream().sorted((a, b) -> Double.compare(score.getOrDefault(b, 0d), score.getOrDefault(a, 0d))).toList();
    return toResultsWithScores(sorted, score, "both");
  }

  private List<SearchResultDto> toResults(List<UUID> ids, String matchType) {
    List<SearchResultDto> out = new ArrayList<>();
    for (UUID id : ids) {
      noteRepository
          .findById(id)
          .ifPresent(
              n ->
                  out.add(
                      new SearchResultDto(
                          n.getId(),
                          n.getTitle(),
                          NoteMapper.bodyPreview(n.getBody()),
                          matchType,
                          0)));
    }
    return out;
  }

  private List<SearchResultDto> toResultsWithScores(
      List<UUID> ids, Map<UUID, Double> scores, String matchType) {
    List<SearchResultDto> out = new ArrayList<>();
    for (UUID id : ids) {
      Note n = noteRepository.findById(id).orElse(null);
      if (n != null) {
        out.add(
            new SearchResultDto(
                n.getId(),
                n.getTitle(),
                NoteMapper.bodyPreview(n.getBody()),
                matchType,
                scores.getOrDefault(id, 0d)));
      }
    }
    return out;
  }
}
