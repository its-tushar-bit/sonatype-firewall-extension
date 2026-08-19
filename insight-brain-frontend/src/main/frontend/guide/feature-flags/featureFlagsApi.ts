/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { notifySessionResponse } from '../auth/sessionExpiration';

const FEATURES_URL = '/rest/product/features';

export async function fetchFeatureFlags(): Promise<string[]> {
  const response = await fetch(FEATURES_URL, { credentials: 'same-origin' });
  notifySessionResponse(response);

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}
