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
    vm.organizations = undefined;
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
      organizations: {},
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
        var storedFilters = data[4].data;
        vm.organizations = angular.copy(data[2]); // copied as we modify objects
        vm.applications = data[0];
        vm.stages = data[1];
        vm.categories = data[3].data;

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

        if (storedFilters) {
          (storedFilters.organizationFilters || []).forEach(function(organizationId) {

            var orgExists = vm.organizations.some(function(organization) {
              return organization.id === organizationId;
            });

            if(orgExists) {
              vm.selected.organizations[organizationId] = true;
              
              vm.applications.forEach(function(application) {
                if (application.organizationId === organizationId) {
                  var appExistsInFilter = (storedFilters.applicationFilters || []).some(function(applicationId) {
                    return application.id === applicationId;
                  });

                  if (!appExistsInFilter) {
                    (storedFilters.applicationFilters || []).push(application.id);
                  }
                }
              });
            }
          });

          (storedFilters.applicationFilters || []).forEach(function(applicationId) {
            vm.selected.applications[applicationId] = true;
          });

          (storedFilters.tagFilters || []).forEach(function(categoryId) {
            vm.selected.categories[categoryId] = true;
          });

          (storedFilters.stageTypeFilters || []).forEach(function(stageId) {
            vm.selected.stages[stageId] = true;
          });

          (storedFilters.policyThreatCategoryFilters || []).forEach(function(policyTypeId) {
            vm.selected.policyTypes[policyTypeId] = true;
          });
          vm.selected.policyThreatLevels = [storedFilters.minPolicyThreatLevel, storedFilters.maxPolicyThreatLevel];
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

          if(!angular.equals(vm.selected.organizations, original)) {
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

          if(!angular.equals(vm.selected.applications, original)) {
            vm.selected.applications = angular.copy(vm.selected.applications);
          }
        });

        savedFilters = angular.copy(vm.selected);
        $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, storedFilters);
      }, function(error) {
        vm.loadError = error;
      });
    }

    function clear() {
      vm.selected = {
        organizations: {},
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
        organizationFilters: createSelectedIdsArray(vm.selected.organizations),
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
