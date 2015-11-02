/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SameOwnerStateNavigationService($state) {
    return {
      goEdit: goEdit,
      refactorStateParams: refactorStateParams
    };

    function goEdit(to, params) {
      var refactoredParams = refactorStateParams(to, params);
      $state.go(refactoredParams.to, refactoredParams.params);
    }

    function refactorStateParams(to, params) {
      var isApp = $state.current.name.indexOf('application') !== -1,
          type = isApp ? 'application' : 'organization',
          ownerId = isApp ? 'applicationPublicId' : 'organizationId';

      to = 'management.edit.' + type + (to ? ('.' + to ) : '');
      params = params || {};

      if ($state.params[ownerId]) {
        params[ownerId] = $state.params[ownerId];
      }

      return {to: to, params: params};
    }
  }

  SameOwnerStateNavigationService.$inject = ['$state'];

  angular.module('owner.manager.module').service('SameOwnerStateNavigationService', SameOwnerStateNavigationService);
}(angular));
