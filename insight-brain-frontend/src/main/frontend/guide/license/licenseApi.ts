/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { notifySessionResponse } from '../auth/sessionExpiration';

const LICENSE_VALIDATE_URL = '/rest/product/license/validate';

export interface LicenseSummary {
  productEdition: string | null;
  products: string[];
}

export async function fetchLicenseSummary(): Promise<LicenseSummary> {
  const response = await fetch(LICENSE_VALIDATE_URL, { credentials: 'same-origin' });
  notifySessionResponse(response);

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  return {
    productEdition: data.productEdition ?? null,
    products: data.products ?? [],
  };
}
