/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerTreeViewController($q, $scope, $state, $stateParams, $http, CLMLocations, organizationStore,
                                   applicationStore, OwnerEditor, PermissionService, ownerConstant, EventNameConstant)
  {
    var vm = this;

    vm.filter = {
      value: ''
    };
    vm.$state = $state;
    vm.rootOrganization = undefined;
    vm.organizations = undefined;

    vm.createApplication = createApplication;
    vm.createOrganization = createOrganization;
    vm.doLoad = doLoad;
    vm.goToOrganizationIfNotSynthetic = goToOrganizationIfNotSynthetic;

    $scope.$watch('vm.filter.value', filter, function(error) {
      vm.error = error;
    });

    $scope.$on(EventNameConstant.OWNER_UPDATED, function(e, owner, type, isNew) {
      owner = angular.copy(owner);
      owner.isVisible = true;
      if (isNew) {
        if (type === ownerConstant.APPLICATION_TYPE) {
          seekOrganizationById(owner.organizationId, function(organization) {
            organization.applications.push(owner);
            vm.selectedParentOrganization = organization;
            organization.isExpanded = true;
          });
        }
        else {
          owner.isExpanded = true;
          owner.applications = [];
          vm.organizations.push(owner);
        }
      }
      else {
        if (type === ownerConstant.APPLICATION_TYPE) {
          seekApplication(owner, function(application) {
            application.name = owner.name;
          });
        }
        else if (owner.id === ownerConstant.ROOT_ORGANIZATION_ID) {
          vm.rootOrganization.name = owner.name;
        }
        else {
          seekOrganizationById(owner.id, function(organization) {
            organization.name = owner.name;
          });
        }
      }
    });

    $scope.$on('owner.deleted', function(e, owner, ownerType) {
      if (ownerType === ownerConstant.APPLICATION_TYPE) {
        seekApplication(owner, function(application, organization, index) {
          organization.applications.splice(index, 1);
        });
      }
      else {
        seekOrganizationById(owner.id, function(organization, index) {
          vm.organizations.splice(index, 1);
        });
      }
    });

    $scope.$on('$stateChangeSuccess', function() {
      redirectIfNecessary();
      vm.selectedParentOrganization = null;
      assignSelectedParentOrganization();
    });

    $scope.$on(EventNameConstant.RELOAD_OWNER_TREE_DATA, doLoad);

    vm.doLoad();

    function doLoad() {
      delete vm.error;
      delete vm.rootOrganization;

      var loadPromises = [
        $http.get(CLMLocations.getOwnerListUrl()),
        PermissionService.isContextAuthorized(['READ'], 'repository_container')
      ];

      $q.all(loadPromises).then(function(results) {
        vm.organizations = results[0].data.organizations;
        vm.showRepositories = results[1];

        for (var i = vm.organizations.length - 1; i >= 0; i--) {
          var organization = vm.organizations[i];
          organization.isVisible = true;
          organization.isExpanded = $state.includes('management.view.organization', {organizationId: organization.id});

          organization.applications.forEach(function(application) {
            application.isVisible = true;
          });

          if (organization.id === ownerConstant.ROOT_ORGANIZATION_ID) {
            vm.rootOrganization = organization;
            vm.organizations.splice(i, 1);
          }
        }

        redirectIfNecessary();

        assignSelectedParentOrganization();
      });
    }

    function redirectIfNecessary() {
      if ($state.is('management.view')) {
        var topOrganization = vm.rootOrganization || vm.organizations.filter(function(org) {
              return !org.synthetic;
            })[0] || vm.organizations[0];
        if (topOrganization) {
          if (topOrganization.synthetic) {
            $state.go('.application', {applicationPublicId: topOrganization.applications[0].publicId});
          } 
          else {
            $state.go('.organization', {organizationId: topOrganization.id});
          }
        }
      }
    }

    function seekApplication(application, fn) {
      vm.organizations.some(function(organization) {
        if (organization.id === application.organizationId) {
          organization.applications.some(function(app, index) {
            if (app.id === application.id) {
              fn(app, organization, index);
              return true;
            }
          });
          return true;
        }
      });
    }

    function seekOrganizationById(organizationId, fn) {
      vm.organizations.some(function(organization, index) {
        if (organization.id === organizationId) {
          fn(organization, index);
          return true;
        }
      });
    }

    function assignSelectedParentOrganization() {
      vm.organizations.some(function(organization) {
        if (isOrganizationChildSelected(organization)) {
          vm.selectedParentOrganization = organization;
          organization.isExpanded = true;
          return true;
        }
      });
    }

    function createApplication(parent) {
      var application = applicationStore.create();
      application.organizationId = parent.id;
      var applications = vm.organizations.map(function(organization) {
        return organization.applications;
      });
      applications = [].concat.apply([], applications);
      OwnerEditor.open(application, ownerConstant.APPLICATION_TYPE, applications);
    }

    function createOrganization() {
      var organizations = vm.organizations.concat(vm.rootOrganization);
      OwnerEditor.open(organizationStore.create(), ownerConstant.ORGANIZATION_TYPE, organizations);
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
          keys: ['name']
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
            keys: ['name']
          });
          filteredApplications = applicationFuse.search(filterValue);
        }

        for (var j = 0; j < organization.applications.length; j++) {
          var application = organization.applications[j];

          application.isVisible = organizationVisible || !filterValue || filterValue.length < 3 ||
              filteredApplications.indexOf(application.id) > -1;
          anyApplicationVisible = anyApplicationVisible || application.isVisible;
        }

        organization.isExpanded = Boolean(filterValue && (filterValue.length < 3 ? false : anyApplicationVisible)) ||
            isOrganizationChildSelected(organization);
        organization.isVisible = organizationVisible || anyApplicationVisible;
      }
    }

    function goToOrganizationIfNotSynthetic(organization) {
      if (!organization.synthetic) {
        $state.go('management.view.organization', {organizationId: organization.id});
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
  }

  OwnerTreeViewController.$inject = [
    '$q', '$scope', '$state', '$stateParams', '$http', 'CLMLocations', 'OrganizationStore', 'ApplicationStore',
    'OwnerEditorService', 'PermissionService', 'owner.constant', 'event.name.constant'
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
