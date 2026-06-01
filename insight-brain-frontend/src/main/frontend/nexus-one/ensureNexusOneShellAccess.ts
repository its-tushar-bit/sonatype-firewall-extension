/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getProductFeaturesUrl, getSessionUrl } from 'MainRoot/util/CLMLocation';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';

/** Feature id from /rest/product/features when {@code PREVIEW_NEXUS_ONE_UI} is enabled. */
const PREVIEW_NEXUS_ONE_UI_FEATURE = 'preview-nexus-one-ui';

/**
 * Verifies master {@code PREVIEW_NEXUS_ONE_UI} is enabled and the user is authenticated.
 * When access is denied, redirects to the classic shell (mirrors {@code NexusOneIndexAccessFilter}).
 *
 * @return {@code true} when the Nexus One SPA may start; {@code false} after redirecting to classic
 */
export async function ensureNexusOneShellAccess(): Promise<boolean> {
  try {
    const [featuresResponse, sessionResponse] = await Promise.all([
      axios.get<string[]>(getProductFeaturesUrl(), { waitForLogin: false }),
      axios.get<{ username?: string }>(getSessionUrl(), { waitForLogin: false }),
    ]);
    const features = featuresResponse.data ?? [];
    const previewEnabled = Array.isArray(features) && features.includes(PREVIEW_NEXUS_ONE_UI_FEATURE);
    const isAuthenticated = Boolean(sessionResponse.data?.username);
    if (previewEnabled && isAuthenticated) {
      return true;
    }
  } catch {
    // Fail-closed — treat errors as denied.
  }

  window.location.replace(bundleIndexUrl('classic'));
  return false;
}
