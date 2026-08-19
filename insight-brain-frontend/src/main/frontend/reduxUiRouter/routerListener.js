/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { UI_ROUTER_ON_FINISH } from './routerActions';
import store from 'MainRoot/reduxConfig/store';

export function initializeRouterListener(transitionService) {
  transitionService.onFinish({}, (transition) => {
    const fromParams = transition.params('from');
    const toParams = transition.params('to');
    const fromState = transition.from();
    const toState = transition.to();

    store.dispatch({
      type: UI_ROUTER_ON_FINISH,
      payload: {
        toState,
        toParams,
        fromState,
        fromParams,
      },
    });
  });
}
