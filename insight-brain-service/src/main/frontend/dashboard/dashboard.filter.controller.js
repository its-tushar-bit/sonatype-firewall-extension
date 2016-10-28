/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DashboardFilterController($rootScope, $scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore,
                                     OrganizationStore, EventNameConstant, SaveFilterModal)
  {
    var vm = this,
        appliedFilter;

    // Available
    vm.organizations = undefined;
    vm.applications = undefined;
    vm.categories = undefined;
    vm.stages = undefined;
    vm.policyTypes = [
      {
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
      }
    ];

    // User selected
    vm.selected = undefined;

    vm.loadError = undefined;
    vm.saveError = undefined;
    vm.savedNamedFilters = [];
    vm.loadErrorFilterName = undefined;

    vm.doLoad = doLoad;
    vm.isDirty = isDirty;
    vm.clear = clear;
    vm.revert = revert;
    vm.applyCurrentFilter = applyCurrentFilter;
    vm.applySavedFilter = applySavedFilter;
    vm.loadFilterFromJson = loadFilterFromJson;
    vm.openSaveFilterModal = openSaveFilterModal;

    vm.doLoad();

    $scope.$on('reloadFilter', function() {
      vm.doLoad();
    });

    function doLoad() {
      delete vm.loadError;

      var promises = [
        ApplicationStore.get(), StageTypeStore.getDashboardStages(), OrganizationStore.get(),
        $http.get(CLMLocations.getApplicationTagsUrl()), $http.get(CLMLocations.getDashboardFilters()),
        $http.get(CLMLocations.getDashboardSavedFilters())
      ];

      $q.all(promises).then(function(data) {
        var activeFilter = data[4].data;
        vm.organizations = angular.copy(data[2]); // copied as we modify objects
        vm.applications = data[0];
        vm.stages = data[1];
        vm.categories = data[3].data;
        vm.savedNamedFilters = data[5].data;

        angular.forEach(vm.categories, function(category) {
          for (var i = 0; i < vm.organizations.length; i++) {
            if (category.organizationId === vm.organizations[i].id) {
              category.owner = vm.organizations[i].name;
              break;
            }
          }
        });

        vm.applications.forEach(function(application) {
          var orgExists = vm.organizations.some(function(organization) {
            return application.organizationId === organization.id;
          });

          if (!orgExists) {
            vm.organizations.push({id: application.organizationId, name: application.organizationName});
          }
        });

        vm.organizations = vm.organizations.filter(function(organization) {
          return organization.id !== 'ROOT_ORGANIZATION_ID';
        });

        if (activeFilter) {
          vm.loadFilterFromJson(activeFilter);
        }

        $scope.$watch('vm.selected.applications', function() {
          var original = angular.copy(vm.selected.organizations);

          vm.organizations.forEach(function(org) {
            var hasApp = false;
            var hasUnselectedApps = vm.applications.some(function(app) {
              if (app.organizationId === org.id) {
                hasApp = true;
              }
              return app.organizationId === org.id && !vm.selected.applications[app.id];
            });
            // remove checkbox if there aren't any selected apps and handle special cases where there are no apps
            // belonging to an org
            if (hasUnselectedApps || (!hasApp && !vm.selected.organizations[org.id])) {
              delete vm.selected.organizations[org.id];
            }
            else {
              vm.selected.organizations[org.id] = true;
            }
          });

          if (!angular.equals(vm.selected.organizations, original)) {
            vm.selected.organizations = angular.copy(vm.selected.organizations);
          }
        });

        $scope.$watch('vm.selected.organizations', function() {
          var original = angular.copy(vm.selected.applications);

          vm.organizations.forEach(function(org) {
            if (vm.selected.organizations[org.id]) {
              // set all applications w/ orgId
              vm.applications.forEach(function(app) {
                if (app.organizationId === org.id) {
                  vm.selected.applications[app.id] = true;
                }
              });
            }
            else {
              // set them to empty if and only if all applications are selected
              var hasUnselectedApps = vm.applications.some(function(app) {
                return app.organizationId === org.id && !vm.selected.applications[app.id];
              });
              if (!hasUnselectedApps) {
                vm.applications.forEach(function(app) {
                  if (app.organizationId === org.id) {
                    delete vm.selected.applications[app.id];
                  }
                });
              }
            }
          });

          if (!angular.equals(vm.selected.applications, original)) {
            vm.selected.applications = angular.copy(vm.selected.applications);
          }
        });

        appliedFilter = angular.copy(vm.selected);
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter);
      }, function(error) {
        vm.loadError = error;
      });
    }

    function clear() {
      resetFilter();
      delete vm.loadErrorFilterName;
    }

    function revert() {
      vm.selected = angular.copy(appliedFilter);
      delete vm.loadErrorFilterName;
    }

    function resetFilter() {
      vm.selected = {
        organizations: {},
        applications: {},
        categories: {},
        stages: {},
        policyTypes: {},
        policyThreatLevels: [2, 10]
      };
    }

    function applyCurrentFilter() {
      delete vm.saveError;
      delete vm.loadErrorFilterName;

      if (!vm.isDirty()) {
        return;
      }

      applyFilter(filterToJson(vm.selected)).then(function() {
        appliedFilter = angular.copy(vm.selected);
      }, function(error) {
        vm.saveError = error;
      });
    }

    function loadFilterFromJson(filterJson) {
      resetFilter();
      (filterJson.organizationFilters || []).forEach(function(organizationId) {

        var orgExists = vm.organizations.some(function(organization) {
          return organization.id === organizationId;
        });

        if (orgExists) {
          vm.selected.organizations[organizationId] = true;

          vm.applications.forEach(function(application) {
            if (application.organizationId === organizationId) {
              var appExistsInFilter = (filterJson.applicationFilters || []).some(function(applicationId) {
                return application.id === applicationId;
              });

              if (!appExistsInFilter) {
                (filterJson.applicationFilters || []).push(application.id);
              }
            }
          });
        }
      });

      (filterJson.applicationFilters || []).forEach(function(applicationId) {
        vm.selected.applications[applicationId] = true;
      });

      (filterJson.tagFilters || []).forEach(function(categoryId) {
        vm.selected.categories[categoryId] = true;
      });

      (filterJson.stageTypeFilters || []).forEach(function(stageId) {
        vm.selected.stages[stageId] = true;
      });

      (filterJson.policyThreatCategoryFilters || []).forEach(function(policyTypeId) {
        vm.selected.policyTypes[policyTypeId] = true;
      });
      vm.selected.policyThreatLevels = [filterJson.minPolicyThreatLevel, filterJson.maxPolicyThreatLevel];
    }

    function applySavedFilter(savedFilter) {
      delete vm.loadErrorFilterName;

      applyFilter(savedFilter.filter).then(function(activeFilter) {
        vm.loadFilterFromJson(activeFilter);
        appliedFilter = angular.copy(vm.selected);
      }, function() {
        vm.loadErrorFilterName = savedFilter.name;
      });
    }

    /**
     * Persists active filter and applies it to dashboard results
     * @param filterJson
     * @returns Promise wrapping active filter json
     */
    function applyFilter(filterJson) {
      return $http.put(CLMLocations.getDashboardFilters(), filterJson).then(function(activeFilter) {
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter.data);
        return activeFilter.data;
      });
    }

    function isDirty() {
      return !angular.equals(vm.selected, appliedFilter);
    }

    function openSaveFilterModal($event) {
      if(vm.isDirty()) {
        $event.stopPropagation();
        return;
      }
      SaveFilterModal.open(filterToJson(vm.selected)).then(refreshSavedFilters);
    }

    function refreshSavedFilters() {
      $http.get(CLMLocations.getDashboardSavedFilters()).then(function(response) {
        vm.savedNamedFilters = response.data;
      });
    }
  }

  function filterToJson(filter) {
    return {
      organizationFilters: Object.keys(filter.organizations),
      applicationFilters: Object.keys(filter.applications),
      policyThreatCategoryFilters: Object.keys(filter.policyTypes),
      stageTypeFilters: Object.keys(filter.stages),
      tagFilters: Object.keys(filter.categories),
      minPolicyThreatLevel: filter.policyThreatLevels[0],
      maxPolicyThreatLevel: filter.policyThreatLevels[1]
    };
  }

  DashboardFilterController.$inject = [
    '$rootScope', '$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore',
    'StageTypeStore', 'OrganizationStore', 'event.name.constant', 'save.filter.modal'
  ];

  angular.module('dashboard.module').controller('dashboard.filter.controller', DashboardFilterController);
}(angular));
