/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const STATE_GO = '@@reduxUiRouter/stateGo';
export const STATE_RELOAD = '@@reduxUiRouter/stateReload';
export const STATE_TRANSITION_TO = '@@reduxUiRouter/transitionTo';

// UI Router transition event can be used in reducers to track router state
export const UI_ROUTER_ON_FINISH = '@@reduxUiRouter/onFinish';

export const stateGo = (to, params, options) => {
  return {
    type: STATE_GO,
    payload: {
      to,
      params,
      options,
    },
  };
};

export const stateReload = (state) => {
  return {
    type: STATE_RELOAD,
    payload: state,
  };
};

export const stateTransitionTo = (to, params, options) => {
  return {
    type: STATE_TRANSITION_TO,
    payload: {
      to,
      params,
      options,
    },
  };
};
