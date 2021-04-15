/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function SameOwnerStateNavigationService($state) {
  return {
    goEdit: goEdit,
    refactorStateParams: {
      edit: refactorStateParams('edit'),
      view: refactorStateParams('view'),
    },
  };

  function goEdit(to, params) {
    var refactoredParams = refactorStateParams('edit')(to, params);
    $state.go(refactoredParams.to, refactoredParams.params);
  }

  function refactorStateParams(ownerState) {
    return function (to, params) {
      var isApp = $state.current.name.indexOf('application') !== -1,
        isRepositories = $state.current.name.indexOf('repositories') !== -1,
        type = isApp
          ? 'application'
          : isRepositories
          ? 'repositories'
          : 'organization',
        ownerId = isApp ? 'applicationPublicId' : 'organizationId';

      to = 'management.' + ownerState + '.' + type + (to ? '.' + to : '');
      params = params || {};

      if ($state.params[ownerId]) {
        params[ownerId] = $state.params[ownerId];
      }

      return { to: to, params: params };
    };
  }
}

SameOwnerStateNavigationService.$inject = ['$state'];
