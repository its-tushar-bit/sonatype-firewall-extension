/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { STATE_GO, STATE_RELOAD, STATE_TRANSITION_TO } from './routerActions';

export default function routerMiddleware($state) {
  return () => (next) => (action) => {
    const { payload } = action;

    switch (action.type) {
      case STATE_GO:
        return $state
          .go(payload.to, payload.params, payload.options)
          .then(next(action));

      case STATE_RELOAD:
        return $state.reload(payload).then(next(action));

      case STATE_TRANSITION_TO:
        return $state
          .transitionTo(payload.to, payload.params, payload.options)
          .then(next(action));

      default:
        return next(action);
    }
  };
}

routerMiddleware.$inject = ['$state'];
