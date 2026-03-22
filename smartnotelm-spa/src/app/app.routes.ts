import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/workbench/workbench.component').then((m) => m.WorkbenchComponent),
  },
];
