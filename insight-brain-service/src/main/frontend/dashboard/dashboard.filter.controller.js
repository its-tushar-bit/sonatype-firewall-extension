/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DashboardFilterController($rootScope, $scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore,
                                     OrganizationStore, EventNameConstant) {
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
    vm.saveError = undefined;

    vm.doLoad = doLoad;
    vm.isDirty = isDirty;
    vm.clear = clear;
    vm.revert = revert;
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
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, storedFilters);
      }, function(error) {
        vm.loadError = error;
      });
    }

    function clear() {
      vm.selected = {
        applications: {},
        categories: {},
        stages: {},
        policyTypes: {},
        policyThreatLevels: [2, 10]
      };
    }

    function revert() {
      vm.selected = angular.copy(savedFilters);
    }

    function save() {
      delete vm.saveError;

      if (!vm.isDirty()) {
        return;
      }

      $http.put(CLMLocations.getDashboardFilters(), {
        applicationFilters: createSelectedIdsArray(vm.selected.applications),
        policyThreatCategoryFilters: createSelectedIdsArray(vm.selected.policyTypes),
        stageTypeFilters: createSelectedIdsArray(vm.selected.stages),
        tagFilters: createSelectedIdsArray(vm.selected.categories),
        minPolicyThreatLevel: vm.selected.policyThreatLevels[0],
        maxPolicyThreatLevel: vm.selected.policyThreatLevels[1]
      }).then(function(storedFilters) {
        savedFilters = angular.copy(vm.selected);
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, storedFilters.data);
      }, function(error) {
        vm.saveError = error;
      });
    }

    function isDirty() {
      return !angular.equals(vm.selected, savedFilters);
    }

    function createSelectedIdsArray(selectedMap) {
      return Object.keys(selectedMap);
    }
  }

  DashboardFilterController.$inject = ['$rootScope', '$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore',
                                       'StageTypeStore', 'OrganizationStore', 'event.name.constant'];

  angular.module('dashboard.module').controller('dashboard.filter.controller', DashboardFilterController);
}(angular));
