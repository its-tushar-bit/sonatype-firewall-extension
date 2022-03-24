/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';

const ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE = 'backend-configurable';

export default function routeStateUtilService($state, ProductFeatures, $ngRedux) {
  const loadServerConfigPromise = ProductFeatures.loadIsUnauthenticatedPagesEnabled()
    .catch(always(false))
    .then((isUnauthenticatedPagesEnabled) => {
      $ngRedux.dispatch(actions.setUnauthenticatedPagesEnabled(isUnauthenticatedPagesEnabled));
    });

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

    if (routeRequiresAuth === ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE) {
      const reduxFlag = $ngRedux.getState().userLogin.loginModalState.isUnauthenticatedPagesEnabled;
      return typeof reduxFlag === 'boolean' ? !reduxFlag : reduxFlag;
    } else {
      return routeRequiresAuth ?? true;
    }
  }

  /**
   * Async query for whether this route requires authentication. This is based on both the route's
   * authenticationRequired flag and the server's enable-unauthenticated-pages config.
   */
  function stateRequiresAuthentication(state = $state.current) {
    const basePromise =
      state.data?.authenticationRequired === ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE
        ? loadServerConfigPromise
        : Promise.resolve();

    return basePromise.then(() => stateRequiresAuthenticationSync(state));
  }

  return { stateRequiresAuthenticationSync, stateRequiresAuthentication };
}

routeStateUtilService.$inject = ['$state', 'ProductFeatures', '$ngRedux'];
