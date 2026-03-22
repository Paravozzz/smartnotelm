import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  GroupDto,
  NoteDetail,
  NoteSummary,
  NoteWrite,
  SearchHit,
  TagDto,
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  listNotes(
    view: string,
    sort: string,
    order: string,
    groupId?: string | null,
    tagId?: string | null
  ): Observable<NoteSummary[]> {
    let p = new HttpParams()
      .set('view', view)
      .set('sort', sort)
      .set('order', order);
    if (groupId) p = p.set('groupId', groupId);
    if (tagId) p = p.set('tagId', tagId);
    return this.http.get<NoteSummary[]>(`${this.base}/notes`, { params: p });
  }

  getNote(id: string): Observable<NoteDetail> {
    return this.http.get<NoteDetail>(`${this.base}/notes/${id}`);
  }

  createNote(body: NoteWrite): Observable<NoteDetail> {
    return this.http.post<NoteDetail>(`${this.base}/notes`, body);
  }

  updateNote(id: string, body: NoteWrite): Observable<NoteDetail> {
    return this.http.put<NoteDetail>(`${this.base}/notes/${id}`, body);
  }

  deleteNote(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/notes/${id}`);
  }

  groupTree(): Observable<GroupDto[]> {
    return this.http.get<GroupDto[]>(`${this.base}/groups/tree`);
  }

  createGroup(name: string, parentId: string | null): Observable<GroupDto> {
    return this.http.post<GroupDto>(`${this.base}/groups`, { name, parentId });
  }

  tags(): Observable<TagDto[]> {
    return this.http.get<TagDto[]>(`${this.base}/tags`);
  }

  search(q: string, mode: string): Observable<SearchHit[]> {
    const p = new HttpParams().set('q', q).set('mode', mode);
    return this.http.get<SearchHit[]>(`${this.base}/search`, { params: p });
  }
}
