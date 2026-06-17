/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchLicensedSolutions } from './licenseApi';
import type { LicensedSolution, SolutionId } from '../layout/ProductSwitcher/productMetadata';

interface LicenseContextValue {
  solutions: LicensedSolution[];
  isLoading: boolean;
  hasError: boolean;
  hasSolution: (solutionId: SolutionId) => boolean;
}

const LicenseContext = createContext<LicenseContextValue | null>(null);

export function LicenseProvider({ children }: { children: ReactNode }) {
  const [solutions, setSolutions] = useState<LicensedSolution[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    fetchLicensedSolutions()
      .then((result) => {
        if (!cancelled) {
          setSolutions(result);
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
    };
  }, []);

  const hasSolution = useCallback(
    (solutionId: SolutionId) => solutions.some((s) => s.id === solutionId),
    [solutions]
  );

  const value = useMemo<LicenseContextValue>(
    () => ({ solutions, isLoading, hasError, hasSolution }),
    [solutions, isLoading, hasError, hasSolution]
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
