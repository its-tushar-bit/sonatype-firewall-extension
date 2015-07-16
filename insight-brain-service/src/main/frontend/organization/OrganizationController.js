/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  angular.module('OrganizationModule', ['ui.router', 'ManagementModule', 'Organization', 'Stores', 'OwnerModule'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('management.organization', {
        parent: 'management',
        url: '/organization',
        controller: 'OrganizationController',
        templateUrl: '../organization-assets/components/organization-navigator.html?' + clmBuildTimestamp
      }).state('management.organization-view', {
        parent: 'management',
        url: '/organization/{organizationId}',
        templateUrl: 'components/owner-summary-view.html?' + clmBuildTimestamp
      }).state('management.organization.view.policies', {
        parent: 'management.organization.view',
        url: '/policies',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.view.policies.new', {
        parent: 'management.organization.view.policies',
        url: '/new',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.view.labels', {
        parent: 'management.organization.view',
        url: '/labels',
        controller: 'LabelController',
        templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.view.labels.new', {
        parent: 'management.organization.view.labels',
        url: '/new',
        controller: 'LabelController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.view.licenses', {
        parent: 'management.organization.view',
        url: '/licenses',
        controller: 'LicenseThreatGroupController',
        templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' +
            clmBuildTimestamp
      }).state('management.organization.view.licenses.new', {
        parent: 'management.organization.view.licenses',
        url: '/new',
        controller: 'LicenseThreatGroupController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy-assets/components/license-threat-group/license-threat-group.html?' +
        clmBuildTimestamp
      }).state('management.organization.view.security', {
        parent: 'management.organization.view',
        url: '/security',
        controller: 'AppSecurityController',
        templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp,
        resolve : {
          isAuthorized : function () {
            return true;
          }
        }
      }).state('management.organization.view.tags', {
        parent: 'management.organization.view',
        url: '/tags',
        controller: 'TagController',
        templateUrl: '../policy-assets/components/tag-editor/tags.html?' + clmBuildTimestamp
      }).state('management.organization.view.tags.new', {
        parent: 'management.organization.view.tags',
        url: '/new',
        controller: 'TagController',
        templateUrl: '../policy-assets/components/tag-editor/tags.html?' + clmBuildTimestamp
      });
    }
  ]);
}());

(function() {
  'use strict';

  var organizationModule = angular.module('Organization',
      ['AngularCommon', 'ApplicationSecurityModule', 'CLMAppLocation', 'CommonServices', 'EditorTools', 'Labels',
       'LicenseThreatGroup', 'Policy', 'ResourceModule', 'Tags', 'ui.router']);

  organizationModule.controller('OrganizationController', [
    '$scope', '$state', '$http', '$location', 'CLMLocations', 'OrganizationStore',
    function($scope, $state, $http, $location, CLMLocations, OrganizationStore) {
      $scope.isCurrentTab = function(tabName) {
        return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
      };

      $scope.$state = $state;

      // Store icon cache timestamps at higher scope so it is not reinstantiated with editor controller
      $scope.organizationIconTimestamp = {};

      $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState) {
        if (toState.data && toState.data.passThroughAlerts && fromState.data && fromState.data.passThroughAlerts) {
          angular.forEach(fromState.data.passThroughAlerts, function(alert) {
            toState.data.passThroughAlerts.push(alert);
          });
        }
      });

      $scope.doLoad = function() {
        $scope.error = null;
        OrganizationStore.get().then(function(results) {
          $scope.organizations = results;
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.doLoad();
    }
  ]);
}());
