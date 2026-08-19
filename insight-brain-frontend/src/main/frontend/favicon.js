/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global CLM_BUILD_TIMESTAMP */

import store from './reduxConfig/store';
import { selectRouterState } from './reduxUiRouter/routerSelectors';

export function initFavicon() {
  const faviconLink = document.getElementById('favicon-svg');

  if (!faviconLink) {
    return;
  }

  store.subscribe(() => {
    const routerState = selectRouterState(store.getState());
    const favicon = routerState?.data?.favicon;

    faviconLink.href = (favicon || 'productIcons/Lifecycle') + '.svg?' + CLM_BUILD_TIMESTAMP;
  });
}
