export interface NoteSummary {
  id: string;
  title: string;
  bodyPreview: string;
  groupId: string | null;
  groupName: string | null;
  tagIds: string[];
  tagNames: string[];
  createdAt: string;
  dueAt: string | null;
}

export interface NoteDetail extends NoteSummary {
  body: string;
  updatedAt: string;
}

export interface NoteWrite {
  title: string;
  body: string;
  groupId: string | null;
  tagIds: string[];
  dueAt: string | null;
}

export interface TagDto {
  id: string;
  name: string;
  system: boolean;
}

export interface GroupDto {
  id: string;
  parentId: string | null;
  name: string;
  children: GroupDto[];
}

export interface SearchHit {
  noteId: string;
  title: string;
  bodyPreview: string;
  matchType: string;
  score: number;
}

export const TODO_TAG_ID = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
