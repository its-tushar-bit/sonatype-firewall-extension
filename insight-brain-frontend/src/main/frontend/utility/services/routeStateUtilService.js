/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function routeStateUtilService($state) {
  return {
    /**
     * Determines whether the specified router state requires the user to be authenticated.  If no parameter is passed,
     * the current state is checked
     */
    stateRequiresAuthentication(state = $state.current) {
      const { data } = state,
        authenticationRequired = data
          ? data.authenticationRequired !== false
          : true;

      return authenticationRequired;
    },
  };
}

routeStateUtilService.$inject = ['$state'];
