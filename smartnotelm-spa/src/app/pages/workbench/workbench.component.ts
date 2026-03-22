import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
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

/** Совместимость: раньше превью хранилось отдельно. */
const LS_PREVIEW = 'tree.showBodyPreview';
const LS_WORKBENCH_UI = 'workbench.ui';
const DRAFT_NEW = 'draft:new-note';

interface WorkbenchUiPrefs {
  viewMode: 'by_group' | 'by_tag' | 'flat';
  sortField: 'created_at' | 'due_at';
  sortOrder: 'asc' | 'desc';
  onlyTodo: boolean;
  showBodyPreview: boolean;
  searchText: boolean;
  searchSemantic: boolean;
}
/** Задержка перед запросом поиска после остановки ввода (мс). */
const SEARCH_DEBOUNCE_MS = 300;

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
  /** Полнотекст + ILIKE на API (по умолчанию включён, вместе с смыслом — режим «оба»). */
  searchText = true;
  /** Векторный поиск на API (по умолчанию включён). */
  searchSemantic = true;
  /** null — фильтр поиска выключен; иначе в дереве только заметки с этими id. */
  searchMatchIds: Set<string> | null = null;
  private readonly search$ = new Subject<string>();

  modalOpen = false;
  modalEditId: string | null = null;
  modalTitle = '';
  modalBody = '';
  modalGroupId: string | null = null;
  modalDue: string | null = null;
  modalTagIds: string[] = [];

  deleteConfirmId: string | null = null;

  newGroupModalOpen = false;
  newGroupName = '';
  /** null — новая группа на верхнем уровне. */
  newGroupParentId: string | null = null;

  ngOnInit(): void {
    this.loadWorkbenchPrefs();
    this.search$
      .pipe(
        debounceTime(SEARCH_DEBOUNCE_MS),
        distinctUntilChanged(),
        switchMap((q) => this.searchMatchState$(q)),
      )
      .subscribe(({ matchedIds }) => {
        this.searchMatchIds = matchedIds;
        this.rebuildTree();
      });
    this.reload();
  }

  private loadWorkbenchPrefs(): void {
    const raw = localStorage.getItem(LS_WORKBENCH_UI);
    if (raw) {
      try {
        const o = JSON.parse(raw) as Partial<WorkbenchUiPrefs>;
        if (o.viewMode === 'by_group' || o.viewMode === 'by_tag' || o.viewMode === 'flat') {
          this.viewMode = o.viewMode;
        }
        if (o.sortField === 'created_at' || o.sortField === 'due_at') {
          this.sortField = o.sortField;
        }
        if (o.sortOrder === 'asc' || o.sortOrder === 'desc') {
          this.sortOrder = o.sortOrder;
        }
        if (typeof o.onlyTodo === 'boolean') {
          this.onlyTodo = o.onlyTodo;
        }
        if (typeof o.showBodyPreview === 'boolean') {
          this.showBodyPreview = o.showBodyPreview;
        }
        if (typeof o.searchText === 'boolean') {
          this.searchText = o.searchText;
        }
        if (typeof o.searchSemantic === 'boolean') {
          this.searchSemantic = o.searchSemantic;
        }
        this.syncLegacyPreviewKey();
        return;
      } catch {
        /* битый JSON — пересоздаём настройки */
      }
    }
    this.showBodyPreview = localStorage.getItem(LS_PREVIEW) === '1';
    this.saveWorkbenchPrefs();
  }

  private saveWorkbenchPrefs(): void {
    const prefs: WorkbenchUiPrefs = {
      viewMode: this.viewMode,
      sortField: this.sortField,
      sortOrder: this.sortOrder,
      onlyTodo: this.onlyTodo,
      showBodyPreview: this.showBodyPreview,
      searchText: this.searchText,
      searchSemantic: this.searchSemantic,
    };
    localStorage.setItem(LS_WORKBENCH_UI, JSON.stringify(prefs));
    this.syncLegacyPreviewKey();
  }

  /** Старый ключ читали до миграции; дублируем запись на случай внешних ссылок. */
  private syncLegacyPreviewKey(): void {
    localStorage.setItem(LS_PREVIEW, this.showBodyPreview ? '1' : '0');
  }

  togglePreview(checked: boolean): void {
    this.showBodyPreview = checked;
    this.saveWorkbenchPrefs();
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
    this.saveWorkbenchPrefs();
    this.api.listNotes(this.viewMode, this.sortField, this.sortOrder).subscribe((n) => this.applyNotes(n));
  }

  onOnlyTodoChange(): void {
    this.saveWorkbenchPrefs();
    this.api.listNotes(this.viewMode, this.sortField, this.sortOrder).subscribe((n) => this.applyNotes(n));
  }

  private rebuildTree(): void {
    const noteSource = this.filteredNotesForSearch();

    if (this.viewMode === 'flat') {
      this.tree = noteSource.map((n) => this.noteNode(n));
      this.syncSelectionWithVisibleNotes(noteSource);
      return;
    }
    if (this.viewMode === 'by_tag') {
      const map = new Map<string, NoteSummary[]>();
      for (const n of noteSource) {
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
      this.syncSelectionWithVisibleNotes(noteSource);
      return;
    }
    let root = this.buildGroupBranch(this.groups, noteSource);
    const ungrouped = noteSource.filter((n) => !n.groupId).map((n) => this.noteNode(n));
    if (ungrouped.length > 0) {
      root = [
        ...root,
        {
          kind: 'group',
          id: '__ungrouped__',
          title: 'Без группы',
          preview: '',
          children: ungrouped,
        },
      ];
    }
    if (this.searchMatchIds !== null) {
      root = this.pruneEmptyGroups(root);
    }
    this.tree = root;
    this.syncSelectionWithVisibleNotes(noteSource);
  }

  private filteredNotesForSearch(): NoteSummary[] {
    if (this.searchMatchIds === null) {
      return this.notes;
    }
    return this.notes.filter((n) => this.searchMatchIds!.has(n.id));
  }

  /** Убирает пустые группы при активном поиске. */
  private pruneEmptyGroups(nodes: TreeNode[]): TreeNode[] {
    const out: TreeNode[] = [];
    for (const n of nodes) {
      if (n.kind === 'note') {
        out.push(n);
      } else {
        const kids = this.pruneEmptyGroups(n.children);
        if (kids.length > 0) {
          out.push({ ...n, children: kids });
        }
      }
    }
    return out;
  }

  private syncSelectionWithVisibleNotes(visible: NoteSummary[]): void {
    if (this.selectedId && !visible.find((n) => n.id === this.selectedId)) {
      this.selectedId = null;
      this.selectedDetail = null;
    }
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

  /** Каждое изменение строки поиска — в поток с debounce (без ожидания Enter). */
  onSearchQueryChange(value: string): void {
    this.search$.next(value.trim());
  }

  /** Немедленный поиск (смена чекбоксов «Текст» / «Смысл»). */
  runSearch(q: string): void {
    this.searchMatchState$(q).subscribe(({ matchedIds }) => {
      this.searchMatchIds = matchedIds;
      this.rebuildTree();
    });
  }

  private searchMatchState$(q: string): Observable<{ matchedIds: Set<string> | null }> {
    const trimmed = q.trim();
    if (!trimmed) {
      return of({ matchedIds: null });
    }
    const mode = this.searchModeParam();
    if (mode === null) {
      return of({ matchedIds: new Set<string>() });
    }
    return this.api.search(trimmed, mode).pipe(
      map((hits) => ({
        matchedIds: new Set<string>(hits.map((h) => h.noteId)),
      })),
    );
  }

  /** Режим для GET /api/search: оба выключены — не искать. */
  private searchModeParam(): 'text' | 'semantic' | 'both' | null {
    if (this.searchText && this.searchSemantic) {
      return 'both';
    }
    if (this.searchText) {
      return 'text';
    }
    if (this.searchSemantic) {
      return 'semantic';
    }
    return null;
  }

  onSearchOptionsChange(): void {
    this.saveWorkbenchPrefs();
    this.runSearch(this.searchQuery.trim());
  }

  selectNote(id: string): void {
    this.selectedId = id;
    this.api.getNote(id).subscribe((d) => (this.selectedDetail = d));
  }

  openNewGroupModal(): void {
    this.newGroupName = 'Новая группа';
    this.newGroupParentId = null;
    this.newGroupModalOpen = true;
  }

  closeNewGroupModal(): void {
    this.newGroupModalOpen = false;
  }

  saveNewGroup(): void {
    const name = this.newGroupName.trim();
    if (!name) {
      return;
    }
    this.api.createGroup(name, this.newGroupParentId).subscribe({
      next: () => {
        this.newGroupModalOpen = false;
        this.reload();
      },
      error: () => window.alert('Не удалось создать группу.'),
    });
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

  /** Родитель для новой группы: корень или любая существующая группа. */
  groupParentOptionsForNewGroup(): { id: string | null; label: string }[] {
    const acc: { id: string | null; label: string }[] = [{ id: null, label: '(корень)' }];
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
