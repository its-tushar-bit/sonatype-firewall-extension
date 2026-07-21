/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { notifySessionResponse } from '../auth/sessionExpiration';
import { AI_DEVELOPER_SOLUTION_ID, GUIDE_SOLUTION_ID } from './solutionIds';
import type { LicensedSolution } from '../layout/ProductSwitcher/productMetadata';

const LICENSED_SOLUTIONS_URL = '/api/v2/solutions/licensed?allowRelativeUrls=true';

export async function fetchLicensedSolutions(): Promise<LicensedSolution[]> {
  const response = await fetch(LICENSED_SOLUTIONS_URL, { credentials: 'same-origin' });
  notifySessionResponse(response);

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  if (!Array.isArray(data)) {
    return [];
  }

  // The AI Developer product is licensed under two SKUs that report different solution ids: the
  // legacy Guide SKU ('guide') and the new AI Developer SKU ('aiDeveloper', added in GUIDE-3124).
  // A license carries at most one of these SKUs, never both, so this mapping never collapses two
  // distinct entries. In Guide they are the same product surface — both must unlock the UI and both
  // render as "AI Developer" — so canonicalize the new id onto 'guide' here, at the single fetch used
  // by the license gate, the revocation refetch, and the product switcher. See solutionIds.
  return data.map((solution: LicensedSolution) =>
    solution?.id === AI_DEVELOPER_SOLUTION_ID ? { ...solution, id: GUIDE_SOLUTION_ID } : solution
  );
}
