/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { find, propEq, isEmpty } from 'ramda';
import { selectSiblings as selectPolicySiblings } from 'MainRoot/OrgsAndPolicies/policySelectors';
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectSiblings as selectApplicationCategoriesSiblings } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSelectors';
import { selectLabelsSiblings } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSelectors';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { selectLicenseThreatGroupSiblings } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';

export default function OwnerDetailTreeViewController(
  $scope,
  $q,
  $http,
  $state,
  CLMContextLocations,
  OrganizationStore,
  LocalRoleService,
  $ngRedux
) {
  var vm = this;

  vm.areAnyCategoriesDefined = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRepositories = CLMContextLocations.isRepositories();
  vm.state = $state;
  vm.ownerName = undefined;
  vm.details = undefined;
  vm.doLoad = doLoad;
  vm.rolesWithoutLocalMembersExist = undefined;
  vm.error = undefined;
  vm.accessState = { isExpanded: vm.state.$current.name.endsWith('access') };
  vm.categoryState = {
    isExpanded: vm.state.$current.name.endsWith('category'),
  };
  vm.labelState = { isExpanded: vm.state.$current.name.endsWith('label') };
  vm.policyState = { isExpanded: vm.state.$current.name.endsWith('policy') };
  vm.ltgState = {
    isExpanded: vm.state.$current.name.endsWith('license-threat-group'),
  };

  vm.$onInit = function () {
    vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
      loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
      loadApplications: applicationActions.loadApplicationsIfNeeded,
      loadApplicableCategories: applicationCategoriesActions.loadApplicableCategories,
    })(vm);

    vm.doLoad();
  };

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    var promises = [$http.get(CLMContextLocations.getOwnerDetailsUrl()), vm.loadProductFeatures()];

    if (vm.isApp) {
      promises.push(vm.loadApplications());
      promises.push(vm.loadApplicableCategories());
    } else if (!vm.isRepositories) {
      promises.push(OrganizationStore.getById(CLMContextLocations.getEntityId()));
    }

    $q.all(promises)
      .then(function (results) {
        vm.details = results[0].data;
        unwrapResult(results[1]);

        if (vm.isApp) {
          const applications = unwrapResult(results[2]);
          const entityId = CLMContextLocations.getEntityId();

          const application = find(propEq('publicId', entityId))(applications);
          if (!application) {
            throw `Could not find an application with ID ${entityId}.`;
          }
          vm.ownerName = application.name;
        }

        var allMembersByRoles = vm.details.roles.membersByRole;
        vm.details.roles = LocalRoleService.getRolesWithLocalMembers(allMembersByRoles);
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(allMembersByRoles).length > 0;

        if (!vm.isRepositories) {
          if (vm.isApp) {
            const applicableCategories = unwrapResult(results[3]);
            vm.areAnyCategoriesDefined = !isEmpty(applicableCategories);
          } else {
            vm.ownerName = results[2].name;
          }
        } else {
          vm.ownerName = 'Repositories';
        }
      })
      .catch((error) => {
        vm.error = error;
      });

    delete vm.error;
  }

  $scope.$on('resource.data.modified', vm.doLoad);
  $scope.$watch('vm.labels', (labels) => {
    if (labels) {
      vm.doLoad();
    }
  });
  $scope.$watch('vm.categories', (categories) => {
    if (categories) {
      vm.doLoad();
    }
  });
  $scope.$watch('vm.policies', (policies) => {
    if (policies) {
      vm.doLoad();
    }
  });
  $scope.$watch('vm.licenseThreatGroups', (licenseThreatGroups) => {
    if (licenseThreatGroups) {
      vm.doLoad();
    }
  });
}

const mapStateToThis = (state) => ({
  labels: selectLabelsSiblings(state),
  categories: selectApplicationCategoriesSiblings(state),
  policies: selectPolicySiblings(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  licenseThreatGroups: selectLicenseThreatGroupSiblings(state),
});

OwnerDetailTreeViewController.$inject = [
  '$scope',
  '$q',
  '$http',
  '$state',
  'CLMContextLocations',
  'OrganizationStore',
  'local.role.service',
  '$ngRedux',
];
