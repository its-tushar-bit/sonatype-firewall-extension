/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, clmBuildTimestamp, AngularUtils */
(function() {
  'use strict';

  var applicationModule = angular.module('ApplicationModule',
      ['ui.router', 'AngularCommon', 'CLMLocation', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels', 'ApplicationSecurityModule', 'Stores', 'OwnerModule'],
      ['$stateProvider', function($stateProvider) {
        $stateProvider.state('management.application-view', {
          parent: 'management',
          url: '/application/{applicationPublicId}',
          templateUrl: 'components/owner-summary-view.html?' + clmBuildTimestamp
        }).state('management.application.view.policies', {
          parent: 'management.application.view',
          url: '/policies',
          controller: 'PolicyController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../policy/components/policy/policy.html?' + clmBuildTimestamp
        }).state('management.application.view.policies.new', {
          parent: 'management.application.view.policies',
          url: '/new',
          controller: 'PolicyController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../policy/components/policy/policy.html?' + clmBuildTimestamp
        }).state('management.application.view.labels', {
          parent: 'management.application.view',
          url: '/labels',
          controller: 'LabelController',
          templateUrl: '../policy/components/label-editor/labels.html?' + clmBuildTimestamp
        }).state('management.application.view.labels.new', {
          parent: 'management.application.view.labels',
          url: '/new',
          controller: 'LabelController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../policy/components/label-editor/labels.html?' + clmBuildTimestamp
        }).state('management.application.view.licenses', {
          parent: 'management.application.view',
          url: '/licenses',
          controller: 'LicenseThreatGroupController',
          templateUrl: '../policy/components/license-threat-group/license-threat-group.html?' + clmBuildTimestamp
        }).state('management.application.view.licenses.new', {
          parent: 'management.application.view.licenses',
          url: '/new',
          controller: 'LicenseThreatGroupController',
          data: {
            passThroughAlerts: []
          },
          templateUrl: '../policy/components/license-threat-group/license-threat-group.html?' + clmBuildTimestamp
        }).state('management.application.view.security', {
          parent: 'management.application.view',
          url: '/security',
          controller: 'AppSecurityController',
          templateUrl: '../policy/components/app-security/app-security.html?' + clmBuildTimestamp,
          resolve : {
            isAuthorized : function () {
              return true;
            }
          }
        }).state('management.application.view.tags', {
          parent: 'management.application.view',
          url: '/tags',
          controller: 'TagApplicationController',
          templateUrl: '../policy/components/tag-editor/tags-application.html?' + clmBuildTimestamp
        });
      }]);

  applicationModule.service('policyEvaluator', ['$q', '$http', 'CLMLocations', function($q, $http, CLMLocations) {
    return {
      evaluate: function(application, policyEvaluation) {
        var deferred = $q.defer();
        var stage = { stageTypeId : policyEvaluation.stageTypeId };
        $http.post(CLMLocations.evaluatePolicyUrl(application.publicId, policyEvaluation.scanId),
                stage).success(function(data) {
          policyEvaluation.time = new Date();
          for (var stageTypeId in application.policyEvaluationsResults) {
            if (stageTypeId === stage.stageTypeId) {
              application.policyEvaluationsResults[stageTypeId] = data;
              break;
            }
          }
          deferred.resolve(data);
        }).error(function(data, status, headers, config) {
              deferred.reject({ data: data, status: status, headers: headers, config: config });
            });
        return deferred.promise;
      }
    };
  }]);

  applicationModule.controller('ContactController', ['$scope', 'contextType', 'contextId', function ($scope, contextType, contextId) {
    $scope.alerts = [];
    $scope.contextId = contextId;
    $scope.contextType = contextType;

    $scope.setQueryResults = function (members, error) {
      $scope.queryResults = members;
      if (error) {
        $scope.alerts = [AngularUtils.toAlert(error)];
      }
    };

    $scope.selectUser = function (user) {
      $scope.$close(user);
    };

    $scope.$watch('queryString', function () {
      // clear the alerts
      $scope.alerts.length = 0;
    });
  }]);
}());
