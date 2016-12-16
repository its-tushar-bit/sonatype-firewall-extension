/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DashboardFilterController($rootScope, $scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore,
                                     OrganizationStore, EventNameConstant, filterService)
  {
    var vm = this,
        appliedFilter,
        appliedFilterName;

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
    vm.showDirtyAsterisk = false;
    vm.clear = clear;
    vm.revert = revert;
    vm.activeFilterName = undefined;
    vm.applyCurrentFilter = applyCurrentFilter;
    vm.loadFilterFromJson = loadFilterFromJson;

    vm.onFilterSelected = onFilterSelected;
    vm.onActiveFilterDeleted = onActiveFilterDeleted;
    vm.onFilterSaved = onFilterSaved;
    vm.toggleManageFiltersDropdown = toggleManageFiltersDropdown;

    function toggleManageFiltersDropdown(open) {
      vm.isManageFiltersDropdownOpen = open;
    }

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
        var activeFilter = data[4].data.filter;
        vm.organizations = angular.copy(data[2]); // copied as we modify objects
        vm.applications = data[0];
        vm.stages = data[1];
        vm.categories = data[3].data;
        vm.savedNamedFilters = data[5].data;
        vm.activeFilterName = appliedFilterName = data[4].data.basedOnFilterName;

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
        var savedNamedFilter = vm.activeFilterName && vm.savedNamedFilters.filter(function(savedFilter) {
          return savedFilter.name === vm.activeFilterName;
        })[0];
        if (savedNamedFilter && !angular.equals(activeFilter, savedNamedFilter.filter)) {
          vm.showDirtyAsterisk = true;
        }
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter);
      }, function(error) {
        vm.loadError = error;
      });
    }

    function clear() {
      resetFilter();
      delete vm.loadErrorFilterName;
      delete vm.activeFilterName;
    }

    function revert() {
      vm.selected = angular.copy(appliedFilter);
      delete vm.loadErrorFilterName;
      vm.activeFilterName = appliedFilterName;
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

      var namedFilter = {
        filter: filterService.filterToJson(vm.selected),
        basedOnFilterName: vm.activeFilterName
      };

      applyFilter(namedFilter).then(function() {
        vm.showDirtyAsterisk = true;
        appliedFilter = angular.copy(vm.selected);
      }, function(error) {
        vm.saveError = error;
      });
    }

    function loadFilterFromJson(filterJson) {
      resetFilter();
      filterJson = angular.copy(filterJson); // copied as we modify app filters
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

    function onFilterSelected(savedFilter) {
      delete vm.loadErrorFilterName;

      savedFilter.basedOnFilterName = savedFilter.name;
      applyFilter(savedFilter).then(function(activeFilter) {
        vm.loadFilterFromJson(activeFilter);
        appliedFilter = angular.copy(vm.selected);
        appliedFilterName = vm.activeFilterName = savedFilter.name;
        vm.showDirtyAsterisk = false;
      }, function() {
        vm.loadErrorFilterName = savedFilter.name;
      });

    }

    function onActiveFilterDeleted() {
      appliedFilterName = vm.activeFilterName = undefined;
    }

    function onFilterSaved(filterName) {
      appliedFilterName = vm.activeFilterName = filterName;
      vm.showDirtyAsterisk = false;
    }

    /**
     * Persists active filter and applies it to dashboard results
     * @param filterJson
     * @returns Promise wrapping active filter json
     */
    function applyFilter(filterJson) {
      return $http.put(CLMLocations.getDashboardFilters(), filterJson).then(function(activeFilter) {
        appliedFilterName = vm.activeFilterName;
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter.data);
        return activeFilter.data;
      });
    }

    function isDirty() {
      return !angular.equals(vm.selected, appliedFilter);
    }
  }

  DashboardFilterController.$inject = [
    '$rootScope', '$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore', 'StageTypeStore', 'OrganizationStore',
    'event.name.constant', 'dashboard.filter.service'
  ];

  angular.module('dashboard.module').controller('dashboard.filter.controller', DashboardFilterController);
}(angular));
