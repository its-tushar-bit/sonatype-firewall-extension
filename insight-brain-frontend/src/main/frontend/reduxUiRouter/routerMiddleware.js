/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { STATE_GO, STATE_RELOAD, STATE_TRANSITION_TO } from './routerActions';

let stateService;

export function setStateService(service) {
  stateService = service;
}

const routerMiddleware = () => (next) => (action) => {
  if (!stateService) {
    return next(action);
  }

  const { payload } = action;
  const isStandaloneFirewall = stateService.includes('firewall');
  const resolvedToState =
    payload && isStandaloneFirewall && !payload.to?.includes('firewall') ? `firewall.${payload.to}` : payload?.to;

  switch (action.type) {
    case STATE_GO:
      return stateService.go(resolvedToState, payload.params, payload.options).then(() => next(action));

    case STATE_RELOAD:
      return stateService.reload(payload).then(() => next(action));

    case STATE_TRANSITION_TO:
      return stateService.transitionTo(resolvedToState, payload.params, payload.options).then(() => next(action));

    default:
      return next(action);
  }
};

export default routerMiddleware;
