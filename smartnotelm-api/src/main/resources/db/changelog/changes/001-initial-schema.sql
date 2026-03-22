-- liquibase formatted sql

-- changeset smartnotelm:1
CREATE EXTENSION IF NOT EXISTS vector;

-- changeset smartnotelm:2
CREATE TABLE note_groups (
    id UUID PRIMARY KEY,
    parent_id UUID REFERENCES note_groups (id) ON DELETE CASCADE,
    name VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_note_groups_parent ON note_groups (parent_id);

-- changeset smartnotelm:3
CREATE TABLE notes (
    id UUID PRIMARY KEY,
    group_id UUID REFERENCES note_groups (id) ON DELETE SET NULL,
    title VARCHAR(2048) NOT NULL DEFAULT '',
    body TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at TIMESTAMPTZ,
    search_vector tsvector
);

CREATE INDEX idx_notes_group ON notes (group_id);
CREATE INDEX idx_notes_created ON notes (created_at);
CREATE INDEX idx_notes_due ON notes (due_at);

-- changeset smartnotelm:4
CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    system BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO tags (id, name, system)
VALUES ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'TODO', TRUE);

-- changeset smartnotelm:5
CREATE TABLE note_tags (
    note_id UUID NOT NULL REFERENCES notes (id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, tag_id)
);

CREATE INDEX idx_note_tags_tag ON note_tags (tag_id);

-- changeset smartnotelm:6
CREATE TABLE note_chunks (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL REFERENCES notes (id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768)
);

CREATE INDEX idx_note_chunks_note ON note_chunks (note_id);

-- changeset smartnotelm:7
CREATE INDEX idx_notes_fts ON notes USING GIN (search_vector);

-- changeset smartnotelm:8
CREATE INDEX idx_note_chunks_hnsw ON note_chunks USING hnsw (embedding vector_cosine_ops);

-- changeset smartnotelm:9 splitStatements:false
CREATE OR REPLACE FUNCTION notes_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.body, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- changeset smartnotelm:10 splitStatements:false
DROP TRIGGER IF EXISTS notes_search_vector_trigger ON notes;
CREATE TRIGGER notes_search_vector_trigger
    BEFORE INSERT OR UPDATE OF title, body ON notes
    FOR EACH ROW
    EXECUTE FUNCTION notes_search_vector_update();
