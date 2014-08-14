/**
 * @license Copyright (c) 2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, AngularUtils, $ */
(function() {
  'use strict';

  var filterModule = angular.module('FilterModule', ['CommonServices', 'CLMLocation', 'Stores']);

  filterModule.controller('FilterController', [
    '$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore', 'StageTypeStore', 'OrganizationStore',
    function($scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore, OrganizationStore) {
      function getEmptyFilters() {
        return {
          applicationIds: [],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [2, 10]
        };
      }

      function applicationNameFor(applicationId) {
        for (var i = 0; i < $scope.applications.length; i++) {
          var application = $scope.applications[i];
          if (application.id === applicationId) {
            return application.name;
          }
        }
      }

      function buildApplicationTooltip() {
        $scope.applicationsTooltip = $.map($scope.filters.applicationIds, applicationNameFor).join('<br/>');
      }

      $scope.doLoad = function() {
        $scope.dirtyFilters = getEmptyFilters();
        $scope.error = null;

        var promises = [
          ApplicationStore.get(),
          StageTypeStore.get(),
          OrganizationStore.get(),
          $http.get(CLMLocations.getApplicationTagsUrl()),
          $http.get(CLMLocations.getDashboardFilters())
        ];

        $q.all(promises).then(function(data) {
          $scope.applications = data[0];
          $scope.stageTypes = angular.copy(data[1]); // Stores should not be modified directly
          $scope.applicationTags = data[3].data;

          for (var i = 0; i < $scope.stageTypes.length; i++) {
            if ($scope.stageTypes[i].id === 'develop') {
              $scope.stageTypes.splice(i, 1);
              break;
            }
          }

          var organizations = data[2];
          angular.forEach($scope.applicationTags, function(tag) {
            for (var i = 0; i < organizations.length; i++) {
              if (tag.organizationId === organizations[i].id) {
                tag.owner = organizations[i].name;
                break;
              }
            }
          });

          $scope.policyThreatTypes = [
            {id: 'SECURITY', name: 'Security'},
            {id: 'LICENSE', name: 'License'},
            {id: 'QUALITY', name: 'Quality'},
            {id: 'OTHER', name: 'Other'}
          ];

          if (data[4].data) {
            $scope.filters = {
              applicationIds: data[4].data.applicationFilters,
              policyThreatTypes: data[4].data.policyThreatCategoryFilters,
              stageTypeIds: data[4].data.stageTypeFilters,
              applicationTagIds: data[4].data.tagFilters,
              policyThreatLevel: [data[4].data.minPolicyThreatLevel, data[4].data.maxPolicyThreatLevel]
            };

            buildApplicationTooltip();

            $scope.dirtyFilters = angular.copy($scope.filters);
          }
          else {
            $scope.filters = getEmptyFilters();
          }
        }, function(error) {
          $scope.filters = getEmptyFilters();
          $scope.fatalError = error;
        });
      };

      $scope.cancel = function() {
        $scope.dirtyFilters = angular.copy($scope.filters);
        $scope.expanded = false;
      };
      $scope.reset = function() {
        $scope.dirtyFilters = getEmptyFilters();
      };
      $scope.save = function() {
        $scope.filters = angular.copy($scope.dirtyFilters);
        $http.put(CLMLocations.getDashboardFilters(), {
          applicationFilters: $scope.filters.applicationIds,
          policyThreatCategoryFilters: $scope.filters.policyThreatTypes,
          stageTypeFilters: $scope.filters.stageTypeIds,
          tagFilters: $scope.filters.applicationTagIds,
          minPolicyThreatLevel: $scope.filters.policyThreatLevel[0],
          maxPolicyThreatLevel: $scope.filters.policyThreatLevel[1]
        }).then(function(){
          buildApplicationTooltip();
          $scope.expanded = false;
        },function(){
          $scope.alerts = [AngularUtils.toAlert(arguments)];
        });
      };

      $scope.doLoad();
    }
  ]);

  filterModule.directive('filterPanel', [
    function() {
      return {
        restrict: 'A',
        replace: true,
        templateUrl: '../dashboard-assets/filter.html?' + clmBuildTimestamp,
        controller: 'FilterController',
        scope: {
          filters: '=',
          fatalError: '='
        }
      };
    }
  ]);
}());
