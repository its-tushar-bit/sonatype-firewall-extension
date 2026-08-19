/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useState } from 'react';
import { getDisplayTheme } from 'MainRoot/util/preferenceStore';

/**
 * P1-F8: lightweight theme controller for the Nexus One Preview UI.
 *
 * Mirrors the contract of nexus-internal Repo's ThemeContext
 * (apps/Repo-enterprise-prototype/src/contexts/ThemeContext.tsx):
 *
 *   - User picks a mode: 'light' | 'dark' | 'system'
 *   - Mode persists in localStorage under `nosc.themeMode` (falls back to Classic
 *     `displayTheme` when unset so first Nexus One visit matches IQ preference)
 *   - `system` resolves to light/dark via `prefers-color-scheme`
 *   - `effectiveTheme` is what we actually apply to Radix <Theme>
 *
 * Why a plain hook (not a Redux slice / context):
 *
 *   - The Nexus One Preview shell mounts Radix <Theme> in multiple
 *     independent places (Dashboard, PlatformHome, ComingSoonPage,
 *     PreviewUiSettingsPage). Each one reads the effective theme
 *     directly; there's no shared parent. A pure hook with localStorage
 *     + `storage` events as the cross-component channel is enough.
 *   - Avoids tangling NOSC code with the IQ Redux store while we're
 *     still pre-merge from a fork.
 *   - Each hook instance syncs to localStorage on change, and listens
 *     to the `storage` event so a change in one component propagates
 *     to all other Preview pages in the same tab (via the synthetic
 *     event we dispatch alongside the localStorage write).
 *
 * Default: 'system'. First-time visitors get the OS preference.
 */

export type ThemeMode = 'light' | 'dark' | 'system';
export type EffectiveTheme = 'light' | 'dark';

const STORAGE_KEY = 'nosc.themeMode';
const STORAGE_EVENT = 'nosc.themeMode.change';

function readModeFromStorage(): ThemeMode {
  if (typeof window === 'undefined') return 'system';
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw === 'light' || raw === 'dark' || raw === 'system') return raw;

    const classic = getDisplayTheme();
    if (classic === 'light' || classic === 'dark' || classic === 'system') return classic;
  } catch {
    // Safari private mode / disabled storage: fall through to default.
  }
  return 'system';
}

function readSystemPrefersDark(): boolean {
  if (typeof window === 'undefined') return false;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

export interface NoscThemeState {
  /** What the user picked. May be 'system'. */
  readonly themeMode: ThemeMode;
  /** Resolved light/dark for passing to Radix <Theme appearance>. */
  readonly effectiveTheme: EffectiveTheme;
  /** Switch the user's preferred mode and persist it. */
  setThemeMode(mode: ThemeMode): void;
  /** Convenience: toggle between light and dark (skips 'system'). */
  toggleTheme(): void;
}

export function useNoscTheme(): NoscThemeState {
  const [themeMode, setThemeModeState] = useState<ThemeMode>(readModeFromStorage);
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(readSystemPrefersDark);

  // Watch system color-scheme preference. Updates effectiveTheme
  // automatically when user has mode = 'system'.
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mql = window.matchMedia('(prefers-color-scheme: dark)');
    const update = (): void => setSystemPrefersDark(mql.matches);
    update();
    mql.addEventListener?.('change', update);
    return () => mql.removeEventListener?.('change', update);
  }, []);

  // Listen for cross-component updates in the same tab. localStorage's
  // built-in 'storage' event only fires for OTHER tabs, so we dispatch
  // our own CustomEvent for same-tab propagation.
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const onCustom = (e: Event): void => {
      const detail = (e as CustomEvent<{ themeMode: ThemeMode }>).detail;
      if (detail?.themeMode) setThemeModeState(detail.themeMode);
    };
    const onStorage = (e: StorageEvent): void => {
      if (e.key === STORAGE_KEY) setThemeModeState(readModeFromStorage());
    };
    window.addEventListener(STORAGE_EVENT, onCustom);
    window.addEventListener('storage', onStorage);
    return () => {
      window.removeEventListener(STORAGE_EVENT, onCustom);
      window.removeEventListener('storage', onStorage);
    };
  }, []);

  const setThemeMode = useCallback((mode: ThemeMode): void => {
    setThemeModeState(mode);
    try {
      window.localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // Storage disabled — change still applies in-memory for this session.
    }
    window.dispatchEvent(new CustomEvent(STORAGE_EVENT, { detail: { themeMode: mode } }));
  }, []);

  const effectiveTheme: EffectiveTheme =
    themeMode === 'system' ? (systemPrefersDark ? 'dark' : 'light') : themeMode;

  const toggleTheme = useCallback((): void => {
    setThemeMode(effectiveTheme === 'dark' ? 'light' : 'dark');
  }, [effectiveTheme, setThemeMode]);

  return { themeMode, effectiveTheme, setThemeMode, toggleTheme };
}
