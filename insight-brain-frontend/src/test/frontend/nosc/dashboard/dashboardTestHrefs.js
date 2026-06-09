/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Expected in-bundle hash hrefs when tests run as the Nexus One SPA. */
export const DASHBOARD_VIOLATIONS_HREF = '#/dashboard/violations';
export const DASHBOARD_APPLICATIONS_HREF = '#/dashboard/applications';

export function setupNexusOneBundleLocation() {
  window.history.replaceState(null, '', '/assets/nexus-one/index.html');
}
