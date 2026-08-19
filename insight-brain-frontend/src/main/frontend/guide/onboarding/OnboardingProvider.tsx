/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

export const STORAGE_KEY = 'aiDeveloper.onboarding.completed';

function readCompleted(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function writeCompleted() {
  try {
    localStorage.setItem(STORAGE_KEY, 'true');
  } catch {
    // ignore (private mode / disabled storage)
  }
}

export interface OnboardingContextValue {
  isOpen: boolean;
  /** Opens the tour imperatively (used by the picker's "Need help?" link). */
  open: () => void;
  /** Persists completion and closes. */
  dismiss: () => void;
}

// Safe defaults so useOnboarding() never throws when a consumer (e.g. the shipped
// PolicyContextModal in its own tests) renders outside the provider.
const OnboardingContext = createContext<OnboardingContextValue>({
  isOpen: false,
  open: () => {},
  dismiss: () => {},
});

export function OnboardingProvider({ children }: { children: React.ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);

  // Defer the first-run check to a post-mount effect so the dialog never flashes
  // before the app shell has painted.
  useEffect(() => {
    if (!readCompleted()) {
      setIsOpen(true);
    }
  }, []);

  const open = useCallback(() => setIsOpen(true), []);
  const dismiss = useCallback(() => {
    writeCompleted();
    setIsOpen(false);
  }, []);

  const value = useMemo(() => ({ isOpen, open, dismiss }), [isOpen, open, dismiss]);

  return <OnboardingContext.Provider value={value}>{children}</OnboardingContext.Provider>;
}

export function useOnboarding(): OnboardingContextValue {
  return useContext(OnboardingContext);
}
