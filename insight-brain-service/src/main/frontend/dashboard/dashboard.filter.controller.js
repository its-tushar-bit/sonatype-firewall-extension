/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, AngularUtils */
(function() {
  'use strict';

  function DashboardFilterController($scope, $http, $q, Dialog, CLMLocations, ApplicationStore, StageTypeStore, OrganizationStore) {
    var vm = this;

    vm.applications = null;
    vm.applicationTags = null;
    vm.stageTypes = null;

    vm.error = undefined;
    vm.fatalError = undefined;
    vm.filtersLoaded = false;

    vm.cancel = cancel;
    vm.doLoad = doLoad;
    vm.reset = reset;
    vm.save = save;

    vm.doLoad();

    $scope.$on('reloadFilter', function(){
      vm.doLoad();
    });

    function doLoad() {
      vm.dirtyFilters = getEmptyFilters();
      vm.error = null;
      vm.fatalError = null;
      vm.filtersLoaded = false;

      var promises = [ApplicationStore.get(), StageTypeStore.getDashboardStages(), OrganizationStore.get(),
                      $http.get(CLMLocations.getApplicationTagsUrl()), $http.get(CLMLocations.getDashboardFilters())];

      $q.all(promises).then(function(data) {
        vm.applications = data[0];
        vm.stageTypes = angular.copy(data[1]); // Stores should not be modified directly
        var organizations = data[2];
        vm.applicationTags = data[3].data;

        // multiSelect specifically uses name & id fields
        vm.stageTypes.forEach(function (stage) {
          stage.name = stage.stageName;
          stage.id = stage.stageTypeId;
        });

        angular.forEach(vm.applicationTags, function(tag) {
          for (var i = 0; i < organizations.length; i++) {
            if (tag.organizationId === organizations[i].id) {
              tag.owner = organizations[i].name;
              break;
            }
          }
        });

        vm.policyThreatTypes = [{
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

        vm.nameMaps = {
          applications: {},
          applicationTags: {},
          stageTypes: {},
          policyTypes: {}
        };

        if (data[4].data) {
          vm.filters = {
            applicationIds: data[4].data.applicationFilters,
            policyThreatTypes: data[4].data.policyThreatCategoryFilters,
            stageTypeIds: data[4].data.stageTypeFilters,
            applicationTagIds: data[4].data.tagFilters,
            policyThreatLevel: [data[4].data.minPolicyThreatLevel, data[4].data.maxPolicyThreatLevel]
          };

          vm.dirtyFilters = angular.copy(vm.filters);
        }
        else {
          vm.filters = getEmptyFilters();
        }

        vm.filtersLoaded = true;
      }, function(error) {
        vm.fatalError = error;
      });
    }

    function cancel() {
      vm.dirtyFilters = angular.copy(vm.filters);
      vm.expanded = false;
    }

    function reset() {
      vm.dirtyFilters = getEmptyFilters();
    }

    function save() {
      vm.filters = angular.copy(vm.dirtyFilters);

      $http.put(CLMLocations.getDashboardFilters(), {
        applicationFilters: vm.filters.applicationIds,
        policyThreatCategoryFilters: vm.filters.policyThreatTypes,
        stageTypeFilters: vm.filters.stageTypeIds,
        tagFilters: vm.filters.applicationTagIds,
        minPolicyThreatLevel: vm.filters.policyThreatLevel[0],
        maxPolicyThreatLevel: vm.filters.policyThreatLevel[1]
      }).then(function(){
        vm.expanded = false;
      }, function() {
        vm.alerts = [AngularUtils.toAlert(arguments)];
      });
    }
  }

  DashboardFilterController.$inject = ['$scope', '$http', '$q', 'Dialog', 'CLMLocations', 'ApplicationStore', 'StageTypeStore', 'OrganizationStore'];

  function getEmptyFilters() {
    return {
      applicationIds: [],
      policyThreatTypes: [],
      stageTypeIds: [],
      applicationTagIds: [],
      policyThreatLevel: [2, 10]
    };
  }

  angular.module('dashboard.module').controller('dashboard.filter.controller', DashboardFilterController);

}());
