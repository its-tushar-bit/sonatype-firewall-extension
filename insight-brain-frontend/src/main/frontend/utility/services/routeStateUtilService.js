/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';
import { actions as userLoginActions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { unwrapResult } from '@reduxjs/toolkit';

export const ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE = 'backend-configurable';
export const QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED =
  'quarantined-component-view-anonymous-access-configurable';

// Private module state - will be initialized from outside
let reduxStore = null;
let loadServerConfigPromise = null;
let loadQuarantinedComponentViewAnonymousAccessConfigPromise = null;

/**
 * Initialize the module with the Redux store dependency.
 * This must be called before using stateRequiresAuthentication or stateRequiresAuthenticationSync.
 * @param {Object} _reduxStore - Redux store
 */
export function initialize(_reduxStore) {
  reduxStore = _reduxStore;

  // Initialize the server config promises only if we have valid dependencies
  if (reduxStore && reduxStore.dispatch) {
    loadServerConfigPromise = reduxStore
      .dispatch(productFeaturesActions.loadIsUnauthenticatedPagesEnabled())
      .then(unwrapResult)
      .then((isUnauthenticatedPagesEnabled) => {
        reduxStore.dispatch(userLoginActions.setUnauthenticatedPagesEnabled(isUnauthenticatedPagesEnabled));
      })
      .catch(always(false));

    loadQuarantinedComponentViewAnonymousAccessConfigPromise = reduxStore
      .dispatch(productFeaturesActions.loadIsQuarantinedComponentViewAnonymousAccessEnabled())
      .then(unwrapResult)
      .then((isQuarantinedComponentViewAnonymousAccessEnabled) => {
        reduxStore.dispatch(
          userLoginActions.setQuarantinedComponentViewAnonymousAccessEnabled(
            isQuarantinedComponentViewAnonymousAccessEnabled
          )
        );
      })
      .catch(always(false));
  } else {
    // Fallback promises for when dependencies are not available
    loadServerConfigPromise = Promise.resolve(false);
    loadQuarantinedComponentViewAnonymousAccessConfigPromise = Promise.resolve(false);
  }
}

/**
 * Synchronous query for whether this route requires authentication. This is based on both the route's
 * authenticationRequired flag and the server's enable-unauthenticated-pages config. This method exists
 * so that calling code can use it to decide whether to perform actions which must be synchronous, such as
 * calling preventDefault on navigation events.
 *
 * @param {Object} state - UI-Router state object (required - do not use router.stateService.current as it may be stale)
 * @return true if the route always requires auth, or if it's up to the server and the server config has already
 * been fetched and is false (unauthenticated access disabled)
 * @return false if the route never requires auth, or if it's up to the server and the server config has already
 * been fetched and is true
 * @return undefined if it's up to the server and the server config fetch has not yet completed
 */
export function stateRequiresAuthenticationSync(state) {
  if (!reduxStore || !state) {
    return true; // Safe default - require authentication if dependencies not available
  }

  const routeRequiresAuth = state.data?.authenticationRequired;

  switch (routeRequiresAuth) {
    case ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE: {
      const reduxFlag = reduxStore.getState().userLogin.loginModalState.isUnauthenticatedPagesEnabled;
      return typeof reduxFlag === 'boolean' ? !reduxFlag : reduxFlag;
    }
    case QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED: {
      const reduxFlag = reduxStore.getState().userLogin.loginModalState
        .isQuarantinedComponentViewAnonymousAccessEnabled;
      return typeof reduxFlag === 'boolean' ? !reduxFlag : reduxFlag;
    }
    default:
      return routeRequiresAuth ?? true;
  }
}

/**
 * Async query for whether this route requires authentication. This is based on both the route's
 * authenticationRequired flag and the server's enable-unauthenticated-pages config.
 *
 * @param {Object} state - UI-Router state object (required - do not use router.stateService.current as it may be stale)
 * @return {Promise<boolean>} Promise that resolves to true if authentication is required
 */
export function stateRequiresAuthentication(state) {
  if (!reduxStore || !state) {
    return Promise.resolve(true); // Safe default - require authentication if dependencies not available
  }

  let basePromise;

  switch (state.data?.authenticationRequired) {
    case ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE:
      basePromise = loadServerConfigPromise;
      break;
    case QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED:
      basePromise = loadQuarantinedComponentViewAnonymousAccessConfigPromise;
      break;
    default:
      basePromise = Promise.resolve();
  }

  return basePromise.then(() => stateRequiresAuthenticationSync(state));
}
