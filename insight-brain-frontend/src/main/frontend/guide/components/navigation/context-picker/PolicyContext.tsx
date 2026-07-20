/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useOwnerAdapter } from './OwnerAdapterProvider';
import { setOwnerScope } from 'GuideRoot/api/ownerScope';
import type { AncestorPathEntry, Owner } from './types';

/** localStorage key holding the persisted owner id — the active policy context (org or app). */
const STORAGE_KEY = 'guide.policyOwner';

/** A single segment of the active selection's breadcrumb (root-to-owner, inclusive of the owner). */
export type PathSegment = AncestorPathEntry;

interface PolicyContextValue {
  /** The selected owner, or `null` for the Root Organization. */
  activeOwner: Owner | null;
  /**
   * Sets the active owner and persists its id to localStorage (rehydrated on reload via
   * resolveOwner). The downstream policy re-fetch on context change is handled by the separate
   * wiring story — not here.
   */
  setActiveOwner: (owner: Owner | null) => void;
  /**
   * Breadcrumb for the current selection: the owner's ancestor path plus the owner itself,
   * root-first. Empty for the Root Organization.
   */
  activePath: PathSegment[];
  /**
   * `false` only while a persisted selection is being resolved on load; `true` otherwise. The
   * scope boundary gates the first Guide data request on this so the request carries the resolved
   * owner rather than firing at root and re-fetching.
   */
  hydrated: boolean;
  isPickerOpen: boolean;
  setIsPickerOpen: (open: boolean) => void;
}

const PolicyContext = createContext<PolicyContextValue | undefined>(undefined);

export function PolicyContextProvider({ children }: { children: ReactNode }) {
  const adapter = useOwnerAdapter();
  const [activeOwner, setActiveOwnerState] = useState<Owner | null>(null);
  const [isPickerOpen, setIsPickerOpen] = useState(false);
  const [hydrated, setHydrated] = useState<boolean>(() => {
    try {
      // Hydrated up-front unless there is a stored id to resolve first.
      return localStorage.getItem(STORAGE_KEY) == null;
    } catch {
      // Storage unavailable → nothing to restore → already hydrated.
      return true;
    }
  });
  // Set once the user makes an explicit selection, so the in-flight mount rehydrate below never
  // clobbers a fresh choice with the previously-stored one when resolveOwner resolves late.
  const userSelectedRef = useRef(false);

  // Persist the selection to localStorage so it survives reloads (rehydrated on mount via
  // resolveOwner). Follows the ThemeProvider localStorage convention. There is no backend endpoint
  // for this — GUIDE-3046 exposes read-only picker data — so the active context lives client-side.
  const setActiveOwner = useCallback((owner: Owner | null) => {
    userSelectedRef.current = true;
    setOwnerScope(owner?.id ?? null);
    setHydrated(true);
    setActiveOwnerState(owner);
    try {
      if (owner) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(owner.id));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // ignore storage errors (e.g. Safari private mode)
    }
  }, []);

  // Rehydrate a persisted selection on mount. resolveOwner returns the owner with its breadcrumb
  // path; a null return (backend 404 → not found or lost permission) clears the stored value and
  // falls back to root. The adapter, not this component, owns the 404 handling.
  useEffect(() => {
    let cancelled = false;
    let storedId: string | null = null;
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      storedId = stored ? (JSON.parse(stored) as string) : null;
    } catch {
      storedId = null;
    }
    if (!storedId) {
      setHydrated(true);
      return;
    }

    adapter
      .resolveOwner(storedId)
      .then((owner) => {
        // Bail if unmounted, or if the user already picked an owner while this call was in flight —
        // their selection (already persisted by setActiveOwner) must win over the restored value.
        if (cancelled || userSelectedRef.current) {
          return;
        }
        if (owner) {
          // Rehydrate only — the value is already in storage, so use the raw setter (no re-write).
          setActiveOwnerState(owner);
          setOwnerScope(owner.id);
        } else {
          // Stale/inaccessible selection — clear it and stay on root.
          try {
            localStorage.removeItem(STORAGE_KEY);
          } catch {
            // ignore storage errors
          }
        }
      })
      .catch(() => {
        // Transient resolve failure (non-404): keep the stored value for a future attempt, stay root.
      })
      .finally(() => {
        if (!cancelled) {
          setHydrated(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [adapter]);

  const activePath = useMemo<PathSegment[]>(() => {
    if (!activeOwner) {
      return [];
    }
    return [
      ...activeOwner.ancestorPath,
      { id: activeOwner.id, name: activeOwner.name, type: activeOwner.type },
    ];
  }, [activeOwner]);

  const value = useMemo<PolicyContextValue>(
    () => ({ activeOwner, setActiveOwner, activePath, isPickerOpen, setIsPickerOpen, hydrated }),
    [activeOwner, setActiveOwner, activePath, isPickerOpen, hydrated]
  );

  return <PolicyContext.Provider value={value}>{children}</PolicyContext.Provider>;
}

export function usePolicyContext(): PolicyContextValue {
  const ctx = useContext(PolicyContext);
  if (!ctx) {
    throw new Error('usePolicyContext must be used within a PolicyContextProvider');
  }
  return ctx;
}
