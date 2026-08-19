/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchLicensedSolutions } from './licenseApi';
import { setLicenseRevocationHandler } from './licenseRevocation';
import { GUIDE_SOLUTION_ID } from './solutionIds';
import type { LicensedSolution, SolutionId } from '../layout/ProductSwitcher/productMetadata';

interface LicenseContextValue {
  solutions: LicensedSolution[];
  isLoading: boolean;
  hasError: boolean;
  hasSolution: (solutionId: SolutionId) => boolean;
  /**
   * Re-fetches the licensed solutions in response to a mid-session Guide revocation marker (see
   * {@code licenseRevocation}) and updates {@link solutions}. Single-flight: concurrent calls
   * collapse onto the in-flight fetch. Because the triggering marker is authoritative, guide is
   * always dropped from {@link solutions} — on both a successful and a failed refetch — so the
   * {@code LicenseGate} swaps in the learn-more page; the refetch still refreshes the other
   * (non-guide) solutions.
   */
  refresh: () => Promise<void>;
}

const LicenseContext = createContext<LicenseContextValue | null>(null);

export function LicenseProvider({ children }: { children: ReactNode }) {
  const [solutions, setSolutions] = useState<LicensedSolution[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  // Guards setters in the async refresh path against an unmounted provider (mirrors the mount
  // effect's `cancelled` flag). Reset on each mount so it survives StrictMode's double-invoke.
  const mountedRef = useRef(true);
  // Holds the in-flight refresh so concurrent revocation triggers collapse to a single fetch.
  const refreshPromiseRef = useRef<Promise<void> | null>(null);
  // Set once a revocation marker has been seen. Guards the initial mount fetch from re-adding guide
  // if it resolves after a revocation refresh has already dropped it — keeps this provider correct
  // independent of how LicenseGate renders during loading.
  const revocationSeenRef = useRef(false);

  useEffect(() => {
    mountedRef.current = true;
    let cancelled = false;

    fetchLicensedSolutions()
      .then((result) => {
        if (!cancelled) {
          // If a revocation arrived while this initial fetch was in flight, the marker is
          // authoritative — drop guide so a slow mount fetch cannot re-add it after refresh()
          // has already removed it.
          setSolutions(
            revocationSeenRef.current ? result.filter((s) => s.id !== GUIDE_SOLUTION_ID) : result
          );
          setHasError(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSolutions([]);
          setHasError(true);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
      mountedRef.current = false;
    };
  }, []);

  const refresh = useCallback((): Promise<void> => {
    // Record that a revocation marker was seen so an initial mount fetch still in flight does not
    // re-add guide after this refresh drops it.
    revocationSeenRef.current = true;

    // Single-flight: reuse the in-flight fetch if a refresh is already running.
    if (refreshPromiseRef.current) {
      return refreshPromiseRef.current;
    }

    setIsLoading(true);
    // NB: fetchLicensedSolutions() hits /api/v2/solutions/licensed via raw fetch (NOT apiFetch),
    // so this refresh can never recurse back into the revocation handler.
    const promise = fetchLicensedSolutions()
      .then((result) => {
        if (mountedRef.current) {
          // refresh() runs only in response to the authoritative revocation marker, so guide is
          // gone regardless of what /solutions/licensed reports. The Guide data API gates on
          // LicensedFeature.GUIDE_SEARCH (SearchLicenseFilter) while /solutions/licensed derives
          // the guide solution from LicensedFeature.GUIDE (SolutionResolver) — independent
          // features — so a successful refetch can still list guide when only GUIDE_SEARCH was
          // lost. Drop guide here too, or LicenseGate would remount the Guide UI and the next data
          // call would 403 again (loop). The refetch still refreshes the other (non-guide)
          // solutions.
          setSolutions(result.filter((s) => s.id !== GUIDE_SOLUTION_ID));
          setHasError(false);
        }
      })
      .catch(() => {
        // Refetch failed: still drop guide (the triggering marker is authoritative) so the user is
        // not left on a Guide UI the backend will only 403. Other solutions are retained as-is.
        if (mountedRef.current) {
          setSolutions((prev) => prev.filter((s) => s.id !== GUIDE_SOLUTION_ID));
        }
      })
      .finally(() => {
        if (mountedRef.current) {
          setIsLoading(false);
        }
        refreshPromiseRef.current = null;
      });

    refreshPromiseRef.current = promise;
    return promise;
  }, []);

  // Register the refresh as the module-level revocation handler so the Guide HTTP client can
  // invoke it on a license-revocation response without threading it through React context.
  useEffect(() => {
    setLicenseRevocationHandler(refresh);
    return () => setLicenseRevocationHandler(null);
  }, [refresh]);

  const hasSolution = useCallback(
    (solutionId: SolutionId) => solutions.some((s) => s.id === solutionId),
    [solutions]
  );

  const value = useMemo<LicenseContextValue>(
    () => ({ solutions, isLoading, hasError, hasSolution, refresh }),
    [solutions, isLoading, hasError, hasSolution, refresh]
  );

  return <LicenseContext.Provider value={value}>{children}</LicenseContext.Provider>;
}

export function useLicense(): LicenseContextValue {
  const context = useContext(LicenseContext);
  if (!context) {
    throw new Error('useLicense must be used within a LicenseProvider');
  }
  return context;
}
