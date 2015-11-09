/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp, AngularUtils, $ */
(function() {
  'use strict';

  var filterModule = angular.module('FilterModule',
      ['CommonServices', 'AngularCommon', 'CLMLocation', 'Stores', 'BootstrapAddons']);

  filterModule.controller('FilterController', [
    '$scope', '$http', '$q', 'Dialog', 'CLMLocations', 'ApplicationStore', 'StageTypeStore', 'OrganizationStore',
    function($scope, $http, $q, Dialog, CLMLocations, ApplicationStore, StageTypeStore, OrganizationStore) {
      //simply used for escaping html strings below
      var utilDom = $('<span></span>');
      function getEmptyFilters() {
        return {
          applicationIds: [],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [2, 10]
        };
      }

      function getStageSortIndex(id) {
        switch (id) {
          case 'develop':
            return 0;
          case 'build':
            return 1;
          case 'stage-release':
            return 2;
          case 'release':
            return 3;
          case 'operate':
            return 4;
          default:
            return 0;
        }
      }

      function getPolicyTypeSortIndex(id) {
        switch (id) {
          case 'security':
            return 0;
          case 'license':
            return 1;
          case 'quality':
            return 2;
          case 'other':
            return 3;
          default:
            return 0;
        }
      }

      function alphaSort(a, b) {
        var aLower = (a ? a : '').toLowerCase(), bLower = (b ? b : '').toLowerCase();
        return aLower < bLower ? -1 : aLower > bLower ? 1 : 0;
      }

      function stageSort(a, b) {
        return getStageSortIndex((a ? a : '').toLowerCase()) -
            getStageSortIndex((b ? b : '').toLowerCase());
      }

      function policyTypeSort(a, b) {
        return getPolicyTypeSortIndex((a ? a : '').toLowerCase()) -
            getPolicyTypeSortIndex((b ? b : '').toLowerCase());
      }

      function buildTooltip(items, nameMap, emptyTip, sortFn, idSort) {
        if (items && items.length) {
          if (idSort && sortFn) {
            items = angular.copy(items).sort(sortFn);
          }
          var mappedItems = $.map(items, function(item){
            return nameMap[item];
          });
          if (!idSort && sortFn) {
            mappedItems.sort(sortFn);
          }

          //a 2nd pass over the list to escape the html _after_ sorting is complete
          mappedItems = $.map(mappedItems, function(item){
            return utilDom.text(item).html();
          });

          return mappedItems.join('<br/>');
        }

        return emptyTip;
      }

      function buildTooltips() {
        $scope.applicationsTooltip = buildTooltip($scope.filters.applicationIds, $scope.nameMaps.applications,
            'All applications', alphaSort);
        $scope.applicationTagsTooltip = buildTooltip($scope.filters.applicationTagIds, $scope.nameMaps.applicationTags,
            'All applications', alphaSort);
        $scope.stageTypesTooltip = buildTooltip($scope.filters.stageTypeIds, $scope.nameMaps.stageTypes,
            'All stage types', stageSort, true);
        $scope.policyTypesTooltip = buildTooltip($scope.filters.policyThreatTypes, $scope.nameMaps.policyTypes,
            'All policy types', policyTypeSort, true);
        $scope.policyThreatLevelsTooltip = ($scope.filters.policyThreatLevel[0] !==
            $scope.filters.policyThreatLevel[1] ? 'Policy threat levels ' + $scope.filters.policyThreatLevel[0] +
            ' through ' + $scope.filters.policyThreatLevel[1] : 'Policy threat level ' +
            $scope.filters.policyThreatLevel[0]);
      }

      function populateNameMap(itemList, nameMap) {
        angular.forEach(itemList, function(item){
          nameMap[item.id] = item.name;
        });
      }

      $scope.doLoad = function() {
        $scope.dirtyFilters = getEmptyFilters();
        $scope.error = null;
        $scope.fatalError = null;
        $scope.filtersLoaded = false;

        var promises = [
          ApplicationStore.get(),
          StageTypeStore.getDashboardStages(),
          OrganizationStore.get(),
          $http.get(CLMLocations.getApplicationTagsUrl()),
          $http.get(CLMLocations.getDashboardFilters())
        ];

        $q.all(promises).then(function(data) {
          $scope.applications = data[0];
          $scope.stageTypes = angular.copy(data[1]); // Stores should not be modified directly
          var organizations = data[2];
          $scope.applicationTags = data[3].data;

          // multiSelect specifically uses name & id fields
          $scope.stageTypes.forEach(function (stage) {
            stage.name = stage.stageName;
            stage.id = stage.stageTypeId;
          });

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

          $scope.nameMaps = {
            applications: {},
            applicationTags: {},
            stageTypes: {},
            policyTypes: {}
          };

          populateNameMap($scope.applications, $scope.nameMaps.applications);
          populateNameMap($scope.stageTypes, $scope.nameMaps.stageTypes);
          populateNameMap($scope.applicationTags, $scope.nameMaps.applicationTags);
          populateNameMap($scope.policyThreatTypes, $scope.nameMaps.policyTypes);

          if (data[4].data) {
            $scope.filters = {
              applicationIds: data[4].data.applicationFilters,
              policyThreatTypes: data[4].data.policyThreatCategoryFilters,
              stageTypeIds: data[4].data.stageTypeFilters,
              applicationTagIds: data[4].data.tagFilters,
              policyThreatLevel: [data[4].data.minPolicyThreatLevel, data[4].data.maxPolicyThreatLevel]
            };

            $scope.dirtyFilters = angular.copy($scope.filters);
          }
          else {
            $scope.filters = getEmptyFilters();
          }

          $scope.filtersLoaded = true;
          buildTooltips();
        }, function(error) {
          $scope.filters = getEmptyFilters();
          $scope.fatalError = error;
          $scope.filtersLoaded = true;
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
          buildTooltips();
          $scope.expanded = false;
        },function(){
          $scope.alerts = [AngularUtils.toAlert(arguments)];
        });
      };

      $scope.togglePanel = function() {
        if ($scope.expanded && !angular.equals($scope.dirtyFilters, $scope.filters)) {
          Dialog.open({
            title: 'Filter Settings Changed',
            body: 'Your filter settings have unsaved changes, apply them now?',
            buttons: [
              {
                name: 'Cancel',
                type: 'cancel',
                click: function() {
                  $scope.cancel();
                }
              },
              {
                name: 'Apply',
                type: 'primary',
                click: function() {
                  $scope.save();
                }
              }
            ]
          });
        } else {
          $scope.expanded = !$scope.expanded;
        }
      };

      $scope.$on('reloadFilter', function(){
        $scope.doLoad();
      });

      $scope.doLoad();
    }
  ]);

  filterModule.directive('filterPanel', [
    function() {
      return {
        restrict: 'A',
        replace: true,
        templateUrl: 'dashboard/filter.html?' + clmBuildTimestamp,
        controller: 'FilterController',
        scope: {
          filters: '=',
          fatalError: '='
        }
      };
    }
  ]);
}());
