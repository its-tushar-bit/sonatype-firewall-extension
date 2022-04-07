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

export default function routeStateUtilService($state, $ngRedux) {
  const loadServerConfigPromise = $ngRedux
    .dispatch(productFeaturesActions.loadIsUnauthenticatedPagesEnabled())
    .then(unwrapResult)
    .then((isUnauthenticatedPagesEnabled) => {
      $ngRedux.dispatch(userLoginActions.setUnauthenticatedPagesEnabled(isUnauthenticatedPagesEnabled));
    })
    .catch(always(false));

  const loadQuarantinedComponentViewAnonymousAccessConfigPromise = $ngRedux
    .dispatch(productFeaturesActions.loadIsQuarantinedComponentViewAnonymousAccessEnabled())
    .then(unwrapResult)
    .then((isQuarantinedComponentViewAnonymousAccessEnabled) => {
      $ngRedux.dispatch(
        userLoginActions.setQuarantinedComponentViewAnonymousAccessEnabled(
          isQuarantinedComponentViewAnonymousAccessEnabled
        )
      );
    })
    .catch(always(false));

  /**
   * Synchronous query for whether this route requires authentication. This is based on both the route's
   * authenticationRequired flag and the server's enable-unauthenticated-pages config. This method exists
   * so that calling code can use it to decide whether to perform actions which must be synchronous, such as
   * calling preventDefault on navigation events.
   *
   * @return true if the route always requires auth, or if it's up to the server and the server config has already
   * been fetched and is false (unauthenticated access disabled)
   * @return false if the route never requires auth, or if it's up to the server and the server config has already
   * been fetched and is true
   * @return undefined if it's up to the server and the server config fetch has not yet completed
   */
  function stateRequiresAuthenticationSync(state = $state.current) {
    const routeRequiresAuth = state.data?.authenticationRequired;

    switch (routeRequiresAuth) {
      case ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE: {
        const reduxFlag = $ngRedux.getState().userLogin.loginModalState.isUnauthenticatedPagesEnabled;
        return typeof reduxFlag === 'boolean' ? !reduxFlag : reduxFlag;
      }
      case QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS_ENABLED: {
        const reduxFlag = $ngRedux.getState().userLogin.loginModalState
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
   */
  function stateRequiresAuthentication(state = $state.current) {
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

  return { stateRequiresAuthenticationSync, stateRequiresAuthentication };
}

routeStateUtilService.$inject = ['$state', '$ngRedux'];
