/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIRouterReact, servicesPlugin, hashLocationPlugin } from '@uirouter/react';

// Create singleton router instance
const router = new UIRouterReact();

// Add plugins
router.plugin(servicesPlugin);
router.plugin(hashLocationPlugin);

// Configure
router.urlService.config.strictMode(false);

// Normalize value-less query params by adding =true
// This is needed because React UI-Router doesn't handle value-less query params identically
const valuelessParamRegex = /(^|&)([^=&#]+)(?=&|$)/g;

function normalizeValuelessQueryParams() {
  const hash = window.location.hash;
  const queryStart = hash.indexOf('?');
  if (queryStart === -1) return;

  const path = hash.substring(0, queryStart);
  const query = hash.substring(queryStart + 1);

  valuelessParamRegex.lastIndex = 0;
  if (valuelessParamRegex.test(query)) {
    valuelessParamRegex.lastIndex = 0;
    const fixedQuery = query.replace(valuelessParamRegex, '$1$2=true');
    window.location.replace(window.location.pathname + window.location.search + path + '?' + fixedQuery);
  }
}

// Normalize on initial load
normalizeValuelessQueryParams();

// Normalize on hash changes
window.addEventListener('hashchange', normalizeValuelessQueryParams);

// Export singleton - all route files will import this
export default router;
