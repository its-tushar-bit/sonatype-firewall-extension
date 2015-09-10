/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerSummaryController($scope, $state, OwnerEditor, ApplicationStore, OrganizationStore) {
    var siblings;

    $scope.doLoad = function() {
      var isApp = $state.current.name.indexOf('application') !== -1, stateIdField = isApp ? 'applicationPublicId'
          : 'organizationId', idField = isApp ? 'publicId' : 'id';

      $scope.type = isApp ? 'application' : 'organization';

      (isApp ? ApplicationStore : OrganizationStore)[$scope.error ? 'refresh' : 'get']().then(function(candidates) {
        siblings = candidates;
        angular.forEach(candidates, function(candidate) {
          if (candidate[idField] === $state.params[stateIdField]) {
            $scope.owner = candidate;
          }
        });

        if (!$scope.owner) {
          $scope.error = 'Unable to locate ' + $scope.type;
        }
      }, function() {
        $scope.error = arguments;
      });

      delete $scope.error;
    };

    $scope.edit = function() {
      OwnerEditor.open($scope.owner, $scope.type, siblings);
    };

    $scope.doLoad();
  }

  OwnerSummaryController.$inject = ['$scope', '$state', 'OwnerEditorService', 'ApplicationStore', 'OrganizationStore'];

  angular
    .module('owner.manager.module')
    .controller('OwnerSummaryController', OwnerSummaryController);

}(angular));
