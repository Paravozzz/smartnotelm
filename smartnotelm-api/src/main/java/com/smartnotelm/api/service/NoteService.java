package com.smartnotelm.api.service;

import com.smartnotelm.api.config.AppProperties;
import com.smartnotelm.api.domain.Note;
import com.smartnotelm.api.domain.NoteGroup;
import com.smartnotelm.api.domain.Tag;
import com.smartnotelm.api.repo.NoteChunkRepository;
import com.smartnotelm.api.repo.NoteGroupRepository;
import com.smartnotelm.api.repo.NoteRepository;
import com.smartnotelm.api.repo.TagRepository;
import com.smartnotelm.api.search.ChunkSplitter;
import com.smartnotelm.api.search.EmbeddingClient;
import com.smartnotelm.api.search.PgVectorUtil;
import com.smartnotelm.api.web.dto.NoteDetailDto;
import com.smartnotelm.api.web.dto.NoteSummaryDto;
import com.smartnotelm.api.web.dto.NoteWriteDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

  private final NoteRepository noteRepository;
  private final NoteGroupRepository groupRepository;
  private final TagRepository tagRepository;
  private final NoteChunkRepository noteChunkRepository;
  private final JdbcTemplate jdbcTemplate;
  private final EmbeddingClient embeddingClient;
  private final AppProperties appProperties;

  @Transactional(readOnly = true)
  public List<NoteSummaryDto> list(
      String view,
      String sort,
      String order,
      UUID groupId,
      UUID tagId) {
    List<Note> notes = noteRepository.findAllForList();
    List<Note> filtered = new ArrayList<>();
    for (Note n : notes) {
      if (groupId != null && (n.getGroup() == null || !groupId.equals(n.getGroup().getId()))) {
        continue;
      }
      if (tagId != null
          && n.getTags().stream().noneMatch(t -> tagId.equals(t.getId()))) {
        continue;
      }
      filtered.add(n);
    }
    Comparator<Note> cmp =
        switch (sort != null ? sort : "created_at") {
          case "due_at" ->
              Comparator.comparing(Note::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()));
          default ->
              Comparator.comparing(Note::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    if ("desc".equalsIgnoreCase(order)) {
      cmp = cmp.reversed();
    }
    filtered.sort(cmp);
    if ("by_tag".equalsIgnoreCase(view)) {
      filtered.sort(
          Comparator.comparing(
              (Note n) ->
                  n.getTags().stream().map(Tag::getName).sorted().findFirst().orElse(""),
              String.CASE_INSENSITIVE_ORDER));
    }
    return filtered.stream().map(NoteMapper::toSummary).toList();
  }

  @Transactional(readOnly = true)
  public NoteDetailDto get(UUID id) {
    Note n =
        noteRepository
            .findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note"));
    return NoteMapper.toDetail(n);
  }

  @Transactional
  public NoteDetailDto create(NoteWriteDto dto) {
    NoteGroup group = resolveGroup(dto.groupId());
    Note n = new Note(UUID.randomUUID(), group, nz(dto.title()), nz(dto.body()));
    n.setDueAt(dto.dueAt());
    applyTags(n, dto.tagIds());
    n = noteRepository.save(n);
    rebuildChunks(n);
    return get(n.getId());
  }

  @Transactional
  public NoteDetailDto update(UUID id, NoteWriteDto dto) {
    Note n =
        noteRepository
            .findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note"));
    n.setTitle(nz(dto.title()));
    n.setBody(nz(dto.body()));
    n.setGroup(resolveGroup(dto.groupId()));
    n.setDueAt(dto.dueAt());
    n.setUpdatedAt(Instant.now());
    n.getTags().clear();
    applyTags(n, dto.tagIds());
    n = noteRepository.save(n);
    rebuildChunks(n);
    return get(n.getId());
  }

  @Transactional
  public void delete(UUID id) {
    if (!noteRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note");
    }
    noteChunkRepository.deleteByNoteId(id);
    noteRepository.deleteById(id);
  }

  private NoteGroup resolveGroup(UUID groupId) {
    if (groupId == null) {
      return null;
    }
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group"));
  }

  private void applyTags(Note n, List<UUID> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }
    Set<Tag> tags = new HashSet<>();
    for (UUID tid : tagIds) {
      Tag t =
          tagRepository
              .findById(tid)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag: " + tid));
      tags.add(t);
    }
    n.getTags().addAll(tags);
  }

  private void rebuildChunks(Note note) {
    noteChunkRepository.deleteByNoteId(note.getId());
    noteChunkRepository.flush();
    List<String> parts = ChunkSplitter.split(note.getBody());
    int idx = 0;
    for (String part : parts) {
      UUID cid = UUID.randomUUID();
      try {
        if (appProperties.embeddings().enabled()) {
          float[] vec = embeddingClient.embed(part);
          if (vec.length != appProperties.embeddings().dimensions()) {
            log.warn(
                "Embedding dim {} != configured {}; storing NULL",
                vec.length,
                appProperties.embeddings().dimensions());
            jdbcTemplate.update(
                "INSERT INTO note_chunks (id, note_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, NULL)",
                cid,
                note.getId(),
                idx,
                part);
          } else {
            jdbcTemplate.update(
                "INSERT INTO note_chunks (id, note_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, CAST(? AS vector))",
                cid,
                note.getId(),
                idx,
                part,
                PgVectorUtil.toLiteral(vec));
          }
        } else {
          jdbcTemplate.update(
              "INSERT INTO note_chunks (id, note_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, NULL)",
              cid,
              note.getId(),
              idx,
              part);
        }
      } catch (Exception e) {
        log.warn("Embedding failed for chunk, storing without vector: {}", e.getMessage());
        jdbcTemplate.update(
            "INSERT INTO note_chunks (id, note_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, NULL)",
            cid,
            note.getId(),
            idx,
            part);
      }
      idx++;
    }
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }
}
