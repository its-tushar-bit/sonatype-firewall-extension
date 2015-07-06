/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function () {
  'use strict';

  var module = angular.module('OwnerModule', ['Stores']);

  module.controller('OwnerSummaryController', ['$scope', '$state', 'ApplicationStore', 'OrganizationStore', function ($scope, $state, ApplicationStore,OrganizationStore) {
    $scope.doLoad = function () {
      var isApp = $state.current.name.indexOf('application') !== -1,
          stateIdField = isApp ? 'applicationPublicId' : 'organizationId',
          idField = isApp ? 'publicId' : 'id';

      $scope.type = isApp ? 'application' : 'organization';
      delete $scope.error;

      (isApp ? ApplicationStore : OrganizationStore).get().then(function (candidates) {
        angular.forEach(candidates, function (candidate) {
          if (candidate[idField] === $state.params[stateIdField]) {
            $scope.owner = candidate;
          }
        });

        if (!$scope.owner) {
          $scope.error = 'Unable to locate ' + $scope.type;
        }
      }, function () {
        $scope.error = arguments;
      });
    };
    $scope.doLoad();
  }]);

  module.directive('ownerImage', ['CLMAppLocations', function (CLMAppLocations) {
    return {
      scope : {
        owner : '=ownerImage'
      },
      template : '<img ng-src="{{ownerUrl}}" ng-if="ownerUrl">',
      link : function (scope) {
        scope.$watch('owner', function () {
          if (scope.owner) {
            scope.ownerUrl = CLMAppLocations.getOwnerImageUrl(scope.owner);
          }
        });

        scope.$on('owner.image.change', function (owner) {
          if (scope.owner === owner && scope.ownerUrl) {
            if (scope.ownerUrl.indexOf('?') !== -1) {
              scope.ownerUrl = scope.ownerUrl.substring(0, scope.ownerUrl.indexOf('?'));
            }
            scope.ownerUrl += '?timestamp=' + Date.now();
          }
        });
      }
    };
  }]);

}());
