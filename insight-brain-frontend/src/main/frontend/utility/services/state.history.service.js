/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function StateHistoryService($rootScope) {
  var service = {
      register: angular.noop, // Fake function used to register this service on app init
      getPreviousState: getPreviousState,
    },
    states = [];

  $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState) {
    if (states.length === 0 || !angular.equals(states[states.length - 1], fromState)) {
      states.push(fromState);
    }
  });

  function getPreviousState() {
    return states.length ? states[states.length - 1] : undefined;
  }

  return service;
}

StateHistoryService.$inject = ['$rootScope'];
