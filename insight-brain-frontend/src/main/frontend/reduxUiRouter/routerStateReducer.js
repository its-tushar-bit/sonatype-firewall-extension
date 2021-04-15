/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UI_ROUTER_ON_FINISH } from './routerActions';

const initState = {
  currentState: {},
  currentParams: {},
  prevState: {},
  prevParams: {},
};

export default function routerStateReducer(state = initState, { type, payload }) {
  switch (type) {
    case UI_ROUTER_ON_FINISH: {
      const { fromState, fromParams, toState, toParams } = payload;
      return {
        currentState: getRouteStateData(toState),
        currentParams: toParams,
        prevState: getRouteStateData(fromState),
        prevParams: fromParams,
      };
    }

    default:
      return state;
  }
}

function getRouteStateData({ name, url, data }) {
  return { name, url, data };
}
