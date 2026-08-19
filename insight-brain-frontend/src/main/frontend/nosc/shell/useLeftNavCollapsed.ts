/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useState } from 'react';

/**
 * Shared collapse state for the Nexus One LeftNav.
 *
 * Backed by localStorage so the choice persists across reloads. Used by
 * both the LeftNav (for its self-contained collapse chevron at the
 * bottom of the rail) and the TopNav (hamburger icon at the far left,
 * matching the Sonatype Guide / Repo Nexus One TopNav pattern).
 *
 * The two consumers stay in sync via a CustomEvent. localStorage's
 * built-in `storage` event only fires for OTHER tabs, so a same-tab
 * change in TopNav must explicitly notify LeftNav (and vice versa).
 *
 * Defensive: localStorage failures (Safari private mode, quota errors)
 * do not crash — the hook degrades to in-memory state for the session.
 */
export const COLLAPSED_KEY = 'nosc.leftnav.collapsed';
const CHANGE_EVENT = 'nosc.leftnav.collapsed.change';

function readCollapsed(): boolean {
  try {
    return typeof window !== 'undefined' && window.localStorage?.getItem(COLLAPSED_KEY) === 'true';
  } catch {
    return false;
  }
}

export function useLeftNavCollapsed(): readonly [boolean, (next: boolean) => void] {
  const [collapsed, setCollapsedState] = useState<boolean>(readCollapsed);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const onCustom = (e: Event): void => {
      const detail = (e as CustomEvent<{ collapsed: boolean }>).detail;
      if (typeof detail?.collapsed === 'boolean') {
        setCollapsedState(detail.collapsed);
      }
    };
    const onStorage = (e: StorageEvent): void => {
      if (e.key === COLLAPSED_KEY) setCollapsedState(readCollapsed());
    };
    window.addEventListener(CHANGE_EVENT, onCustom);
    window.addEventListener('storage', onStorage);
    return () => {
      window.removeEventListener(CHANGE_EVENT, onCustom);
      window.removeEventListener('storage', onStorage);
    };
  }, []);

  const setCollapsed = useCallback((next: boolean): void => {
    setCollapsedState(next);
    try {
      window.localStorage?.setItem(COLLAPSED_KEY, next ? 'true' : 'false');
    } catch {
      /* swallow */
    }
    window.dispatchEvent(new CustomEvent(CHANGE_EVENT, { detail: { collapsed: next } }));
  }, []);

  return [collapsed, setCollapsed];
}
