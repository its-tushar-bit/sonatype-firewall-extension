/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsSourceControlSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './owner.tree.view.directive.html';

function OwnerTreeViewController(
  $q,
  $scope,
  $state,
  $stateParams,
  $http,
  $ngRedux,
  CLMLocations,
  OwnerEditor,
  PermissionService,
  ownerConstant,
  EventNameConstant,
  fuzzyFilter,
  scmOnboardingActions
) {
  var vm = this;

  vm.filter = {
    value: '',
  };
  vm.$state = $state;
  vm.rootOrganization = undefined;
  vm.organizations = undefined;

  vm.createApplication = createApplication;
  vm.createOrganization = createOrganization;
  vm.doLoad = doLoad;
  vm.goToOrganizationIfNotSynthetic = goToOrganizationIfNotSynthetic;
  vm.handleOrganizationTwistyClick = handleOrganizationTwistyClick;
  vm.scmProviderIcon = undefined;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    ...scmOnboardingActions,
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
  })(vm);

  $scope.$watch('vm.filter.value', filter, function (error) {
    vm.error = error;
  });

  $scope.$on(EventNameConstant.OWNER_UPDATED, function (e, owner, type, isNew) {
    owner = angular.copy(owner);
    owner.isVisible = true;
    if (isNew) {
      if (type === ownerConstant.APPLICATION_TYPE) {
        seekOrganizationById(owner.organizationId, function (organization) {
          organization.applications.push(owner);
          vm.selectedParentOrganization = organization;
          organization.isExpanded = true;
        });
      } else {
        owner.isExpanded = true;
        owner.applications = [];
        vm.organizations.push(owner);
      }
    } else {
      if (type === ownerConstant.APPLICATION_TYPE) {
        seekApplication(owner, function (application) {
          application.name = owner.name;
        });
      } else if (owner.id === ownerConstant.ROOT_ORGANIZATION_ID) {
        vm.rootOrganization.name = owner.name;
      } else {
        seekOrganizationById(owner.id, function (organization) {
          organization.name = owner.name;
        });
      }
    }
  });

  $scope.$on('owner.deleted', function (e, owner, ownerType) {
    if (ownerType === ownerConstant.APPLICATION_TYPE) {
      seekApplication(owner, function (application, organization, index) {
        organization.applications.splice(index, 1);
      });
    } else {
      seekOrganizationById(owner.id, function (organization, index) {
        vm.organizations.splice(index, 1);
      });
    }
  });

  $scope.$on('$stateChangeSuccess', function () {
    redirectIfNecessary();
    vm.selectedParentOrganization = null;
    assignSelectedParentOrganization();
  });

  $scope.$on(EventNameConstant.RELOAD_OWNER_TREE_DATA, doLoad);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  vm.doLoad();

  function doLoad() {
    delete vm.error;
    delete vm.rootOrganization;

    var loadPromises = [
      $http.get(CLMLocations.getOwnerListUrl()),
      PermissionService.isContextAuthorized(['READ'], 'repository_container'),
      vm.loadProductFeatures(),
    ];

    $q.all(loadPromises).then(
      function (results) {
        unwrapResult(results[2]);

        vm.organizations = results[0].data.organizations;
        vm.showRepositories = results[1];

        for (var i = vm.organizations.length - 1; i >= 0; i--) {
          var organization = vm.organizations[i];
          organization.isVisible = true;
          organization.isExpanded = $state.includes('management.view.organization', {
            organizationId: organization.id,
          });

          organization.applications.forEach(function (application) {
            application.isVisible = true;
            calculateApplicationIcon(application);
          });

          if (organization.id === ownerConstant.ROOT_ORGANIZATION_ID) {
            vm.rootOrganization = organization;
            vm.organizations.splice(i, 1);
          }
        }
        redirectIfNecessary(true);

        assignSelectedParentOrganization();
      },
      function (error) {
        vm.error = error;
      }
    );
  }

  function calculateApplicationIcon(application) {
    if (application && application.provider && application.repositoryUrl) {
      let icon = application.provider;
      if (icon === 'azure') {
        // no Font Awesome icon for Azure, use Microsoft once FA v5 is available (eg: React migration)
        // see: https://github.com/FortAwesome/Font-Awesome/issues/14058
        icon = 'git';
      }
      application.icon = icon;
    }
  }

  function redirectIfNecessary(replaceLastHistoryRecord) {
    if ($state.is('management.view')) {
      var topOrganization =
        vm.rootOrganization ||
        vm.organizations.filter(function (org) {
          return !org.synthetic;
        })[0] ||
        vm.organizations[0];
      if (topOrganization) {
        var options = { location: replaceLastHistoryRecord ? 'replace' : true };
        if (topOrganization.synthetic) {
          $state.go('.application', { applicationPublicId: topOrganization.applications[0].publicId }, options);
        } else {
          $state.go('.organization', { organizationId: topOrganization.id }, options);
        }
      }
    }
  }

  function seekApplication(application, fn) {
    vm.organizations.some(function (organization) {
      if (organization.id === application.organizationId) {
        organization.applications.some(function (app, index) {
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
    vm.organizations.some(function (organization, index) {
      if (organization.id === organizationId) {
        fn(organization, index);
        return true;
      }
    });
  }

  function assignSelectedParentOrganization() {
    vm.organizations.some(function (organization) {
      if (isOrganizationChildSelected(organization)) {
        vm.selectedParentOrganization = organization;
        organization.isExpanded = true;
        return true;
      }
    });
  }

  function createApplication(parent) {
    const application = {
      id: null,
      publicId: null,
      name: null,
      organizationId: parent.id,
      organizationName: parent.name,
      contact: null,
      isNew: true,
    };
    let applications = vm.organizations.map(function (organization) {
      return organization.applications;
    });
    applications = [].concat.apply([], applications);
    OwnerEditor.open(application, ownerConstant.APPLICATION_TYPE, applications);
  }

  function createOrganization() {
    const organization = {
      id: null,
      name: null,
      isNew: true,
    };
    var organizations = vm.organizations.concat(vm.rootOrganization);
    OwnerEditor.open(organization, ownerConstant.ORGANIZATION_TYPE, organizations);
  }

  function filter() {
    if (!vm.organizations) {
      return;
    }

    var filterValue = vm.filter.value;
    var filteredOrganizations = [];
    if (filterValue && filterValue.length >= 3) {
      filteredOrganizations = fuzzyFilter(vm.organizations, filterValue, 'name', 'id');
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
        filteredApplications = fuzzyFilter(organization.applications, filterValue, 'name', 'id');
      }

      for (var j = 0; j < organization.applications.length; j++) {
        var application = organization.applications[j];

        application.isVisible =
          organizationVisible ||
          !filterValue ||
          filterValue.length < 3 ||
          filteredApplications.indexOf(application.id) > -1;
        anyApplicationVisible = anyApplicationVisible || application.isVisible;
      }

      organization.isExpanded =
        Boolean(filterValue && (filterValue.length < 3 ? false : anyApplicationVisible)) ||
        isOrganizationChildSelected(organization);
      organization.isVisible = organizationVisible || anyApplicationVisible;
    }
  }

  function goToOrganizationIfNotSynthetic(organization) {
    if (!organization.synthetic) {
      $state.go('management.view.organization', {
        organizationId: organization.id,
      });
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

  function handleOrganizationTwistyClick(evt, organization) {
    var stateIsThisOrg = vm.$state.includes('management.view.organization', {
        organizationId: organization.id,
      }),
      selectedParentIsThisOrg = vm.selectedParentOrganization && vm.selectedParentOrganization.id === organization.id;

    evt.preventDefault();
    evt.stopPropagation();

    organization.isExpanded = stateIsThisOrg || selectedParentIsThisOrg || !organization.isExpanded;
  }

  function mapStateToThis(state) {
    return {
      isSourceControlSupported: selectIsSourceControlSupported(state),
    };
  }
}

OwnerTreeViewController.$inject = [
  '$q',
  '$scope',
  '$state',
  '$stateParams',
  '$http',
  '$ngRedux',
  'CLMLocations',
  'OwnerEditorService',
  'PermissionService',
  'owner.constant',
  'event.name.constant',
  'fuzzyFilter',
  'scmOnboardingActions',
  'SourceControlService',
];

export default function ownerTreeView() {
  return {
    template,
    controller: OwnerTreeViewController,
    controllerAs: 'vm',
  };
}
