/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, AngularUtils */
(function() {
  'use strict';

  function DashboardFilterController($scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore,
                                     OrganizationStore) {
    var vm = this,
        savedFilters;

    // Available
    vm.applications = undefined;
    vm.categories = undefined;
    vm.stages = undefined;
    vm.policyTypes = [{
      id: 'SECURITY',
      name: 'Security'
    }, {
      id: 'LICENSE',
      name: 'License'
    }, {
      id: 'QUALITY',
      name: 'Quality'
    }, {
      id: 'OTHER',
      name: 'Other'
    }];

    // User selected
    vm.selected = {
      applications: {},
      categories: {},
      policyThreatLevels: [2, 10],
      policyTypes: {},
      stages: {}
    };

    vm.loadError = undefined;

    vm.doLoad = doLoad;
    vm.reset = reset;
    vm.save = save;

    vm.doLoad();

    $scope.$on('reloadFilter', function(){
      vm.doLoad();
    });

    function doLoad() {
      delete vm.loadError;

      var promises = [ApplicationStore.get(), StageTypeStore.getDashboardStages(), OrganizationStore.get(),
                      $http.get(CLMLocations.getApplicationTagsUrl()), $http.get(CLMLocations.getDashboardFilters())];

      $q.all(promises).then(function(data) {
        var organizations = data[2],
            storedFilters = data[4].data;
        vm.applications = data[0];
        vm.stages = data[1];
        vm.categories = data[3].data;

        angular.forEach(vm.categories, function(category) {
          for (var i = 0; i < organizations.length; i++) {
            if (category.organizationId === organizations[i].id) {
              category.owner = organizations[i].name;
              break;
            }
          }
        });

        if (storedFilters) {
          storedFilters.applicationFilters.forEach(function(applicationId) {
            vm.selected.applications[applicationId] = true;
          });

          storedFilters.tagFilters.forEach(function(categoryId) {
            vm.selected.categories[categoryId] = true;
          });

          storedFilters.stageTypeFilters.forEach(function(stageId) {
            vm.selected.stages[stageId] = true;
          });

          storedFilters.policyThreatCategoryFilters.forEach(function(policyTypeId) {
            vm.selected.policyTypes[policyTypeId] = true;
          });
          vm.selected.policyThreatLevels = [storedFilters.minPolicyThreatLevel, storedFilters.maxPolicyThreatLevel];
        }
        savedFilters = angular.copy(vm.selected);
      }, function(error) {
        vm.loadError = error;
      });
    }

    function reset() {
      vm.selected = angular.copy(savedFilters);
    }

    function save() {
      $http.put(CLMLocations.getDashboardFilters(), {
        applicationFilters: createSelectedIdsArray(vm.selected.applications),
        policyThreatCategoryFilters: createSelectedIdsArray(vm.selected.policyTypes),
        stageTypeFilters: createSelectedIdsArray(vm.selected.stages),
        tagFilters: createSelectedIdsArray(vm.selected.categories),
        minPolicyThreatLevel: vm.selected.policyThreatLevels[0],
        maxPolicyThreatLevel: vm.selected.policyThreatLevels[1]
      }).then(function() {
        savedFilters = angular.copy(vm.selected);
      }, function() {
        vm.alerts = [AngularUtils.toAlert(arguments)];
      });
    }

    function createSelectedIdsArray(selectedMap) {
      return Object.keys(selectedMap).filter(function(id) {
        return selectedMap[id];
      });
    }
  }

  DashboardFilterController.$inject = ['$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore', 'StageTypeStore',
                                       'OrganizationStore'];

  angular.module('dashboard.module').controller('dashboard.filter.controller', DashboardFilterController);
}());
