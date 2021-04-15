/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { UI_ROUTER_ON_FINISH } from './routerActions';

export default function routerListener($transitions, $ngRedux) {
  $transitions.onFinish({}, (transition) =>
    $ngRedux.dispatch({
      type: UI_ROUTER_ON_FINISH,
      payload: {
        toState: transition.to(),
        toParams: transition.params('to'),
        fromState: transition.from(),
        fromParams: transition.params('from'),
      },
    })
  );
}

routerListener.$inject = ['$transitions', '$ngRedux'];
