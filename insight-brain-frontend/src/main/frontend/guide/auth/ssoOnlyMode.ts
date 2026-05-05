/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const ENABLE_SSO_ONLY_URL = '/rest/product/features/enableSsoOnly';
const SSO_ONLY_FEATURE_ID = 'enable-sso-only';

export async function fetchIsSsoOnlyEnabled(): Promise<boolean> {
  try {
    const response = await fetch(ENABLE_SSO_ONLY_URL, { credentials: 'same-origin' });
    if (!response.ok) return false;
    const features: string[] = await response.json();
    return features.includes(SSO_ONLY_FEATURE_ID);
  } catch {
    return false;
  }
}
