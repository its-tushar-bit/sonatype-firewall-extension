/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchLicenseSummary } from './licenseApi';
import type { ProductGroup } from './licenseProducts';

interface LicenseContextValue {
  products: string[];
  isLoading: boolean;
  hasLicenseFor: (productGroup: ProductGroup) => boolean;
}

const LicenseContext = createContext<LicenseContextValue | null>(null);

export function LicenseProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetchLicenseSummary()
      .then((summary) => {
        if (!cancelled) {
          setProducts(summary.products);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProducts([]);
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

  const hasLicenseFor = useCallback(
    (productGroup: ProductGroup) => productGroup.some((p) => products.includes(p)),
    [products]
  );

  const value = useMemo<LicenseContextValue>(
    () => ({ products, isLoading, hasLicenseFor }),
    [products, isLoading, hasLicenseFor]
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
