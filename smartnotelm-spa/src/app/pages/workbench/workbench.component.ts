import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { GroupDto, NoteDetail, NoteSummary, NoteWrite, TagDto, TODO_TAG_ID } from '../../models/models';
import { ApiService } from '../../services/api.service';

interface TreeNode {
  kind: 'group' | 'note';
  id: string;
  title: string;
  preview: string;
  children: TreeNode[];
  note?: NoteSummary;
}

const LS_PREVIEW = 'tree.showBodyPreview';
const DRAFT_NEW = 'draft:new-note';

@Component({
  selector: 'app-workbench',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workbench.component.html',
  styleUrl: './workbench.component.scss',
})
export class WorkbenchComponent implements OnInit {
  private readonly api = inject(ApiService);

  viewMode: 'by_group' | 'by_tag' | 'flat' = 'by_group';
  sortField: 'created_at' | 'due_at' = 'created_at';
  sortOrder: 'asc' | 'desc' = 'desc';
  showBodyPreview = false;
  onlyTodo = false;

  notes: NoteSummary[] = [];
  groups: GroupDto[] = [];
  tags: TagDto[] = [];
  tree: TreeNode[] = [];
  selectedId: string | null = null;
  selectedDetail: NoteDetail | null = null;

  searchQuery = '';
  searchMode: 'text' | 'semantic' | 'both' = 'both';
  searchHits: { noteId: string; title: string }[] = [];
  private readonly search$ = new Subject<string>();

  modalOpen = false;
  modalEditId: string | null = null;
  modalTitle = '';
  modalBody = '';
  modalGroupId: string | null = null;
  modalDue: string | null = null;
  modalTagIds: string[] = [];

  deleteConfirmId: string | null = null;

  ngOnInit(): void {
    this.showBodyPreview = localStorage.getItem(LS_PREVIEW) === '1';
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((q) => this.runSearch(q));
    this.reload();
  }

  togglePreview(checked: boolean): void {
    this.showBodyPreview = checked;
    localStorage.setItem(LS_PREVIEW, checked ? '1' : '0');
  }

  reload(): void {
    forkJoin({
      notes: this.api.listNotes(this.viewMode, this.sortField, this.sortOrder),
      groups: this.api.groupTree(),
      tags: this.api.tags(),
    }).subscribe(({ notes, groups, tags }) => {
      this.groups = groups;
      this.tags = tags;
      this.applyNotes(notes);
    });
  }

  private applyNotes(raw: NoteSummary[]): void {
    let list = raw;
    if (this.onlyTodo) {
      list = list.filter((n) => n.tagIds.includes(TODO_TAG_ID));
    }
    this.notes = list;
    this.rebuildTree();
    if (this.selectedId && !list.find((n) => n.id === this.selectedId)) {
      this.selectedId = null;
      this.selectedDetail = null;
    }
  }

  onViewSortChange(): void {
    this.api.listNotes(this.viewMode, this.sortField, this.sortOrder).subscribe((n) => this.applyNotes(n));
  }

  onOnlyTodoChange(): void {
    this.api.listNotes(this.viewMode, this.sortField, this.sortOrder).subscribe((n) => this.applyNotes(n));
  }

  private rebuildTree(): void {
    if (this.viewMode === 'flat') {
      this.tree = this.notes.map((n) => this.noteNode(n));
      return;
    }
    if (this.viewMode === 'by_tag') {
      const map = new Map<string, NoteSummary[]>();
      for (const n of this.notes) {
        const key =
          n.tagNames.length > 0 ? [...n.tagNames].sort((a, b) => a.localeCompare(b))[0] : '(без тега)';
        if (!map.has(key)) map.set(key, []);
        map.get(key)!.push(n);
      }
      const keys = [...map.keys()].sort((a, b) => a.localeCompare(b));
      this.tree = keys.map((k) => ({
        kind: 'group' as const,
        id: 'tag:' + k,
        title: k,
        preview: '',
        children: map.get(k)!.map((n) => this.noteNode(n)),
      }));
      return;
    }
    const root = this.buildGroupBranch(this.groups, this.notes);
    const ungrouped = this.notes
      .filter((n) => !n.groupId)
      .map((n) => this.noteNode(n));
    if (ungrouped.length > 0) {
      root.push({
        kind: 'group',
        id: '__ungrouped__',
        title: 'Без группы',
        preview: '',
        children: ungrouped,
      });
    }
    this.tree = root;
  }

  private buildGroupBranch(gs: GroupDto[], allNotes: NoteSummary[]): TreeNode[] {
    const out: TreeNode[] = [];
    for (const g of gs) {
      const childGroups = this.buildGroupBranch(g.children, allNotes);
      const here = allNotes.filter((n) => n.groupId === g.id).map((n) => this.noteNode(n));
      const children = [...childGroups, ...here];
      out.push({
        kind: 'group',
        id: g.id,
        title: g.name,
        preview: '',
        children,
      });
    }
    return out;
  }

  private noteNode(n: NoteSummary): TreeNode {
    return {
      kind: 'note',
      id: n.id,
      title: n.title || '(без заголовка)',
      preview: n.bodyPreview,
      children: [],
      note: n,
    };
  }

  onSearchInput(value: string): void {
    this.searchQuery = value;
    this.search$.next(value.trim());
  }

  runSearch(q: string): void {
    if (!q) {
      this.searchHits = [];
      return;
    }
    this.api.search(q, this.searchMode).subscribe((hits) => {
      this.searchHits = hits.map((h) => ({ noteId: h.noteId, title: h.title }));
    });
  }

  selectNote(id: string): void {
    this.selectedId = id;
    this.api.getNote(id).subscribe((d) => (this.selectedDetail = d));
    this.searchHits = [];
  }

  pickSearchHit(noteId: string): void {
    this.selectNote(noteId);
    this.searchQuery = '';
    this.searchHits = [];
  }

  openAddModal(): void {
    this.modalEditId = null;
    const raw = localStorage.getItem(DRAFT_NEW);
    if (raw) {
      try {
        const d = JSON.parse(raw) as NoteWrite & { title?: string; body?: string };
        this.modalTitle = d.title ?? '';
        this.modalBody = d.body ?? '';
        this.modalGroupId = d.groupId ?? null;
        this.modalDue = d.dueAt ? d.dueAt.slice(0, 16) : null;
        this.modalTagIds = d.tagIds ?? [];
      } catch {
        this.resetModalForm();
      }
    } else {
      this.resetModalForm();
    }
    this.modalOpen = true;
  }

  openEditModal(n: NoteSummary): void {
    this.modalEditId = n.id;
    const key = 'draft:note:' + n.id;
    const raw = localStorage.getItem(key);
    if (raw) {
      try {
        const d = JSON.parse(raw) as NoteWrite;
        this.modalTitle = d.title ?? '';
        this.modalBody = d.body ?? '';
        this.modalGroupId = d.groupId ?? null;
        this.modalDue = d.dueAt ? d.dueAt.slice(0, 16) : null;
        this.modalTagIds = [...(d.tagIds ?? [])];
        this.modalOpen = true;
      } catch {
        this.loadNoteIntoModalThenOpen(n.id);
      }
    } else {
      this.loadNoteIntoModalThenOpen(n.id);
    }
  }

  private loadNoteIntoModalThenOpen(id: string): void {
    this.api.getNote(id).subscribe((d) => {
      this.modalTitle = d.title;
      this.modalBody = d.body;
      this.modalGroupId = d.groupId;
      this.modalDue = d.dueAt ? d.dueAt.slice(0, 16) : null;
      this.modalTagIds = [...d.tagIds];
      this.modalOpen = true;
    });
  }

  private resetModalForm(): void {
    this.modalTitle = '';
    this.modalBody = '';
    this.modalGroupId = null;
    this.modalDue = null;
    this.modalTagIds = [];
  }

  closeModal(saved: boolean): void {
    if (!saved) {
      const payload: NoteWrite = {
        title: this.modalTitle,
        body: this.modalBody,
        groupId: this.modalGroupId,
        tagIds: [...this.modalTagIds],
        dueAt: this.modalDue ? new Date(this.modalDue).toISOString() : null,
      };
      if (this.modalEditId) {
        localStorage.setItem('draft:note:' + this.modalEditId, JSON.stringify(payload));
      } else {
        localStorage.setItem(DRAFT_NEW, JSON.stringify(payload));
      }
    } else {
      if (this.modalEditId) {
        localStorage.removeItem('draft:note:' + this.modalEditId);
      } else {
        localStorage.removeItem(DRAFT_NEW);
      }
    }
    this.modalOpen = false;
  }

  saveModal(): void {
    const payload: NoteWrite = {
      title: this.modalTitle,
      body: this.modalBody,
      groupId: this.modalGroupId,
      tagIds: [...this.modalTagIds],
      dueAt: this.modalDue ? new Date(this.modalDue).toISOString() : null,
    };
    const req =
      this.modalEditId == null
        ? this.api.createNote(payload)
        : this.api.updateNote(this.modalEditId, payload);
    req.subscribe({
      next: (d) => {
        this.closeModal(true);
        this.reload();
        this.selectNote(d.id);
      },
    });
  }

  requestDelete(id: string): void {
    this.deleteConfirmId = id;
  }

  confirmDelete(): void {
    if (!this.deleteConfirmId) return;
    const id = this.deleteConfirmId;
    this.deleteConfirmId = null;
    this.api.deleteNote(id).subscribe(() => {
      if (this.selectedId === id) {
        this.selectedId = null;
        this.selectedDetail = null;
      }
      this.reload();
    });
  }

  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  toggleModalTag(tagId: string, checked: boolean): void {
    if (checked) {
      if (!this.modalTagIds.includes(tagId)) this.modalTagIds = [...this.modalTagIds, tagId];
    } else {
      this.modalTagIds = this.modalTagIds.filter((t) => t !== tagId);
    }
  }

  tagChecked(tagId: string): boolean {
    return this.modalTagIds.includes(tagId);
  }

  flatGroups(): { id: string | null; label: string }[] {
    const acc: { id: string | null; label: string }[] = [{ id: null, label: '(без группы)' }];
    const walk = (nodes: GroupDto[], prefix: string) => {
      for (const g of nodes) {
        acc.push({ id: g.id, label: prefix + g.name });
        walk(g.children, prefix + g.name + ' / ');
      }
    };
    walk(this.groups, '');
    return acc;
  }
}
