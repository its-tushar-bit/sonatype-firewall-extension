/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { notifySessionResponse } from '../auth/sessionExpiration';
import type { LicensedSolution } from '../layout/ProductSwitcher/productMetadata';

const LICENSED_SOLUTIONS_URL = '/api/v2/solutions/licensed?allowRelativeUrls=true';

export async function fetchLicensedSolutions(): Promise<LicensedSolution[]> {
  const response = await fetch(LICENSED_SOLUTIONS_URL, { credentials: 'same-origin' });
  notifySessionResponse(response);

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}
