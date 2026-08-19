/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { useLicense } from 'GuideRoot/license/LicenseProvider';
import { groupAndSortLicensedSolutions, type LicensedProduct } from './productMetadata';

export interface UseLicensedProductsResult {
  products: LicensedProduct[];
  loading: boolean;
  error: boolean;
}

/**
 * Derives the ProductSwitcher's grouped/sorted product list from the licensed solutions already
 * fetched by {@link LicenseProvider}. Both the license gate and the switcher read the same
 * /api/v2/solutions/licensed response from a single source, so the endpoint is fetched once.
 */
export function useLicensedProducts(): UseLicensedProductsResult {
  const { solutions, isLoading, hasError } = useLicense();
  const products = useMemo(() => groupAndSortLicensedSolutions(solutions), [solutions]);
  return { products, loading: isLoading, error: hasError };
}
