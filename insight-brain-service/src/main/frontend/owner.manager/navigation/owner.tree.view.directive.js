/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerTreeViewController($q, $scope, $state, $stateParams, $timeout, organizationStore, applicationStore,
    OwnerEditor, PermissionService)
  {
    var vm = this, organizations, applications, organizationWatcher, applicationWatcher,
        lastOrganizations = [], lastApplications = [];

    vm.filter = {
      value: ''
    };
    vm.$state = $state;

    vm.createApplication = createApplication;
    vm.createOrganization = createOrganization;
    vm.doLoad = doLoad;
    vm.goToOrganizationIfNotSynthetic = goToOrganizationIfNotSynthetic;

    $scope.$watch('vm.filter.value', filter, function(error) {
      vm.error = error;
    });
    $scope.$on('$destroy', function() {
      clearCollectionWatchers();
    });

    vm.doLoad();

    function assignSelectedParentOrganization() {
      vm.organizations.some(function(organization){
        if (isOrganizationChildSelected(organization)) {
          vm.selectedParentOrganization = organization;
          return true;
        }
      });
    }

    function applicationsCollectionChanged() {
      var found,
          difference = getCollectionDifference(applications, lastApplications);

      if (difference.removed) {
        difference.removed.some(function(removedApplication) {
          vm.organizations.some(function(organization) {
            found = false;
            if (removedApplication.organizationId === organization.id) {
              organization.applications.some(function(application, applicationIndex) {
                if (removedApplication.id === application.id) {
                  organization.applications.splice(applicationIndex, 1);
                  found = true;
                  return found;
                }
              });
            }
            return found;
          });
        });
      }
      if (difference.added) {
        var touchedOrganizations = {};
        difference.added.forEach(function(addedApplication) {
          found = false;
          vm.organizations.some(function(organization) {
            if (addedApplication.organizationId === organization.id) {
              found = true;
              organization.applications.push(newApplication(addedApplication));
              touchedOrganizations[organization.id] = organization;
              return found;
            }
          });
          // Create synthetic organizations for application parents which the user does not have permissions
          // These do not need to be backed by a Resource as the user cannot edit them
          if (!found) {
            var syntheticOrganization = newOrganization({
              id: addedApplication.organizationId,
              name: addedApplication.organizationName
            });
            syntheticOrganization.synthetic = true;
            syntheticOrganization.applications.push(newApplication(addedApplication));
            vm.organizations.push(syntheticOrganization);
            touchedOrganizations[syntheticOrganization.id] = syntheticOrganization;
          }
        });

        for (var key in touchedOrganizations) {
          if (touchedOrganizations.hasOwnProperty(key)) {
            touchedOrganizations[key].isExpanded = $state.includes('management.view.organization',
                {organizationId: touchedOrganizations[key].id}) || isOrganizationChildSelected(touchedOrganizations[key]);
          }
        }
      }

      lastApplications = angular.copy(applications);
    }

    function clearCollectionWatchers() {
      if (organizationWatcher) {
        organizationWatcher();
      }
      if (applicationWatcher) {
        applicationWatcher();
      }
    }

    function createApplication(parent) {
      var application = applicationStore.create();
      application.organizationId = parent.id;
      OwnerEditor.open(application, 'application', applications);
    }

    function createOrganization() {
      OwnerEditor.open(organizationStore.create(), 'organization', organizations);
    }

    function doLoad() {
      delete vm.error;
      delete vm.rootOrganization;
      clearCollectionWatchers();

      var loadPromises = [
        organizationStore.refresh(),
        applicationStore.refresh(),
        PermissionService.isContextAuthorized(['READ'], 'repository_container')
      ];

      $q.all(loadPromises).then(function(results) {
        vm.organizations = [];
        organizations = results[0];
        applications = results[1];
        vm.showRepositories = results[2];

        organizationsCollectionChanged();
        applicationsCollectionChanged();
        assignSelectedParentOrganization();

        $scope.$on('$stateChangeSuccess', function () {
          vm.selectedParentOrganization = null;
          assignSelectedParentOrganization();
        });

        // Apply this after first digest to prevent collection changed event on first load
        $timeout(function() {
          organizationWatcher = $scope.$watch(function() {
            return organizations.length;
          }, organizationsCollectionChanged);
          applicationWatcher = $scope.$watch(function() {
            return applications.length;
          }, applicationsCollectionChanged);
        });
      }, function(error) {
        vm.error = error;
      });
    }

    function filter() {
      if (!vm.organizations) {
        return;
      }

      var filterValue = vm.filter.value;
      var filteredOrganizations = [];
      if (filterValue && filterValue.length >= 3) {
        var organizationFuse = new Fuse(vm.organizations, {
          id: 'id',
          threshold: 0.3,
          keys: [ 'name' ]
        });

        filteredOrganizations = organizationFuse.search(filterValue);
      }

      for (var i = 0; i < vm.organizations.length; i++) {
        var organization = vm.organizations[i],
            organizationVisible = false,
            anyApplicationVisible = false,
            filteredApplications;

        if (!filterValue || filterValue.length < 3 || filteredOrganizations.indexOf(organization.id) > -1) {
          organizationVisible = true;
        }

        if (filterValue && filterValue.length >= 3) {
          var applicationFuse = new Fuse(organization.applications, {
            id: 'id',
            threshold: 0.3,
            keys: [ 'name' ]
          });
          filteredApplications = applicationFuse.search(filterValue);
        }

        for (var j = 0; j < organization.applications.length; j++) {
          var application = organization.applications[j];

          application.isVisible = organizationVisible || !filterValue || filterValue.length < 3 ||
          filteredApplications.indexOf(application.id) > -1;
          anyApplicationVisible = anyApplicationVisible || application.isVisible;
        }

        organization.isExpanded = !filterValue ||
        filterValue.length < 3 ? organization.isExpanded : anyApplicationVisible;
        organization.isVisible = organizationVisible || anyApplicationVisible;
      }
    }

    function getCollectionDifference(newCollection, oldCollection) {
      var removedOwners, addedOwners;

      if (oldCollection.length > newCollection.length) {
        var newCollectionIds = {};
        newCollection.forEach(function(newOwner) {
          newCollectionIds[newOwner.id] = true;
        });
        removedOwners = oldCollection.filter(function(oldOwner) {
          return !newCollectionIds[oldOwner.id];
        });
      } else {
        var oldCollectionIds = {};
        oldCollection.forEach(function(oldOwner) {
          oldCollectionIds[oldOwner.id] = true;
        });
        addedOwners = newCollection.filter(function(newOwner) {
          return !oldCollectionIds[newOwner.id];
        });
      }

      return {
        added: addedOwners,
        removed: removedOwners
      };
    }

    function goToOrganizationIfNotSynthetic(organization) {
      if (!organization.synthetic) {
        $state.go('management.view.organization', { organizationId: organization.id});
      }
    }

    function isOrganizationChildSelected(organization) {
      var isApplicationState = $state.includes('management.view.application');
      if (!isApplicationState) {
        return false;
      }

      for (var i = 0; i < organization.applications.length; i++) {
        var application = organization.applications[i];
        var isApplicationViewed = $stateParams.applicationPublicId === application.publicId;
        if (isApplicationViewed) {
          return true;
        }
      }

      return false;
    }

    function newApplication(applicationResource) {
      var application = {
        id: applicationResource.id,
        name: applicationResource.name,
        organizationId: applicationResource.organizationId,
        publicId: applicationResource.publicId,
        isVisible: true
      };

      $scope.$watch(function() {
        return applicationResource.name;
      }, function(newApplicationName) {
        application.name = newApplicationName;
      });

      return application;
    }

    function newOrganization(organizationResource) {
      var organization = {
        id: organizationResource.id,
        name: organizationResource.name,
        parentOrganizationId: organizationResource.parentOrganizationId,
        applications: [],
        isVisible: true,
        isExpanded: $state.includes('management.view.organization', {organizationId: organizationResource.id})
      };

      $scope.$watch(function() {
        return organizationResource.name;
      }, function(newOrganizationName) {
        organization.name = newOrganizationName;
      });

      return organization;
    }

    function organizationsCollectionChanged() {
      var difference = getCollectionDifference(organizations, lastOrganizations);

      if (difference.removed) {
        difference.removed.forEach(function(removedOrganization) {
          vm.organizations.some(function(organization, organizationIndex) {
            if (removedOrganization.id === organization.id) {
              vm.organizations.splice(organizationIndex, 1);
              return true;
            }
          });
        });
      }
      if (difference.added) {
        difference.added.forEach(function(addedOrganization) {
          vm.organizations.push(newOrganization(addedOrganization));
        });
      }

      lastOrganizations = angular.copy(organizations);

      //set root org then dump it from the list
      vm.organizations.some(function(organization, index) {
        if (!organization.parentOrganizationId && !organization.synthetic) {
          vm.rootOrganization = organization;
          vm.organizations.splice(index,1);
          return true;
        }
      });
    }
  }
  OwnerTreeViewController.$inject = [
    '$q', '$scope', '$state', '$stateParams', '$timeout', 'OrganizationStore', 'ApplicationStore', 'OwnerEditorService',
    'PermissionService'
  ];

  angular
      .module('owner.manager.module')
      .directive('ownerTreeView', ownerTreeView);

  function ownerTreeView() {
    return {
      templateUrl: 'owner.manager/navigation/owner.tree.view.directive.html?' + clmBuildTimestamp,
      controller: OwnerTreeViewController,
      controllerAs: 'vm'
    };
  }
}(angular));
