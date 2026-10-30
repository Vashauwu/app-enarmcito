import { DOCUMENT } from '@angular/common';
import { Injectable, effect, inject, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';

/** Clave compartida con el script de `index.html` que aplica el tema antes de que arranque Angular. */
const STORAGE_KEY = 'enarmcito.tema';

/**
 * Tema claro, oscuro o el del sistema. El tema Material 3 está definido con `color-scheme`, así que basta con
 * fijar esa propiedad en `<html>`. La preferencia se guarda sólo en este navegador.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly root = inject(DOCUMENT).documentElement;
  readonly preference = signal<ThemePreference>(readPreference());

  constructor() {
    effect(() => {
      const preference = this.preference();
      this.root.style.colorScheme = preference === 'system' ? 'light dark' : preference;
      try {
        localStorage.setItem(STORAGE_KEY, preference);
      } catch {
        // Sin almacenamiento disponible: el tema aplica sólo a esta visita.
      }
    });
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
  }
}

function readPreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'light' || stored === 'dark' ? stored : 'system';
  } catch {
    return 'system';
  }
}
