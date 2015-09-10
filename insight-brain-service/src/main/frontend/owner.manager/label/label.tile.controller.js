/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelTileController($state, ApplicationStore, OrganizationStore) {
    var vm = this;

    getOwner();

    function getOwner() {
      var isApp = $state.current.name.indexOf('application') !== -1,
          stateIdField = isApp ? 'applicationPublicId' : 'organizationId',
          idField = isApp ? 'publicId' : 'id';

      var type = isApp ? 'application' : 'organization';

      (isApp ? ApplicationStore : OrganizationStore)[vm.error ? 'refresh' : 'get']().then(function(candidates) {
        angular.forEach(candidates, function(candidate) {
          if (candidate[idField] === $state.params[stateIdField]) {
            vm.owner = candidate;
          }
        });

        if (!vm.owner) {
          vm.error = 'Unable to locate ' + type;
        }
      }, function() {
        vm.error = arguments;
      });

      delete vm.error;
    }
  }

  LabelTileController.$inject = ['$state', 'ApplicationStore', 'OrganizationStore'];

  angular
      .module('owner.manager.module')
      .controller('LabelTileController', LabelTileController);
}(angular));
