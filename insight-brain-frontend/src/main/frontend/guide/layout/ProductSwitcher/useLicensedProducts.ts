/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useState } from 'react';
import { useAuth } from 'GuideRoot/auth/AuthProvider';
import {
  groupAndSortLicensedSolutions,
  type LicensedProduct,
  type LicensedSolution,
} from './productMetadata';

const LICENSED_SOLUTIONS_URL = '/api/v2/solutions/licensed?allowRelativeUrls=true';

export interface UseLicensedProductsResult {
  products: LicensedProduct[];
  loading: boolean;
  error: boolean;
}

export function useLicensedProducts(): UseLicensedProductsResult {
  const { authFetch } = useAuth();
  const [products, setProducts] = useState<LicensedProduct[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<boolean>(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await authFetch(LICENSED_SOLUTIONS_URL);
        if (!response.ok) {
          throw new Error(`Licensed solutions request failed: ${response.status}`);
        }
        const body = (await response.json()) as LicensedSolution[];
        if (cancelled) return;
        setProducts(groupAndSortLicensedSolutions(body));
        setError(false);
      } catch (e) {
        if (cancelled) return;
        // eslint-disable-next-line no-console
        console.error('Failed to load licensed solutions', e);
        setProducts([]);
        setError(true);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [authFetch]);

  return { products, loading, error };
}
