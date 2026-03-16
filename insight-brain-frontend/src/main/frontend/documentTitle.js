/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import store from './reduxConfig/store';
import { selectRouterState } from './reduxUiRouter/routerSelectors';

export function initDocumentTitle() {
  store.subscribe(() => {
    const routerState = selectRouterState(store.getState());
    const title = routerState?.data?.title;
    const product = routerState?.data?.product;

    document.title = (title ? title + ' - ' : '') + (product || 'Lifecycle');
  });
}
