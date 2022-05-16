/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isEmpty, propEq, find } from 'ramda';
import { unwrapResult } from '@reduxjs/toolkit';
import { selectSiblings as selectPolicySiblings } from 'MainRoot/OrgsAndPolicies/policySelectors';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectSiblings as selectApplicationCategoriesSiblings } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSelectors';
import { selectLabelsSiblings } from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function OwnerDetailTreeViewController(
  $scope,
  $q,
  $http,
  $state,
  CLMContextLocations,
  LocalRoleService,
  $ngRedux
) {
  var vm = this;

  vm.areAnyCategoriesDefined = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRepositories = CLMContextLocations.isRepositories();
  vm.state = $state;
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

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
    loadApplications: applicationActions.loadApplications,
    loadOrganizations: organizationsActions.loadOrganizations,
    loadApplicableCategories: applicationCategoriesActions.loadApplicableCategories,
    setSelectedOwner: rootActions.setSelectedOwner,
  })(vm);

  vm.doLoad();

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    var promises = [$http.get(CLMContextLocations.getOwnerDetailsUrl()), vm.loadProductFeatures()];

    if (vm.isApp) {
      promises.push(vm.loadApplications());
      promises.push(vm.loadApplicableCategories());
    } else if (!vm.isRepositories) {
      promises.push(vm.loadOrganizations());
    }

    $q.all(promises)
      .then(function (results) {
        vm.details = results[0].data;
        unwrapResult(results[1]);

        var allMembersByRoles = vm.details.roles.membersByRole;
        vm.details.roles = LocalRoleService.getRolesWithLocalMembers(allMembersByRoles);
        vm.rolesWithoutLocalMembersExist = LocalRoleService.getRolesWithoutLocalMembers(allMembersByRoles).length > 0;

        if (!vm.isRepositories) {
          const siblings = unwrapResult(results[2]);
          const entityId = CLMContextLocations.getEntityId();
          const owner = find(propEq(vm.isApp ? 'publicId' : 'id', entityId))(siblings);

          if (!owner) {
            throw `Could not find an ${vm.isApp ? 'application' : 'organization'} with ID ${entityId}.`;
          }

          vm.setSelectedOwner(owner);

          if (vm.isApp) {
            const applicableCategories = unwrapResult(results[3]);
            vm.areAnyCategoriesDefined = !isEmpty(applicableCategories);
          }
        } else {
          vm.setSelectedOwner({ name: 'Repositories' });
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
}

export const mapStateToThis = (state) => ({
  labels: selectLabelsSiblings(state),
  categories: selectApplicationCategoriesSiblings(state),
  policies: selectPolicySiblings(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  ownerName: selectSelectedOwnerName(state),
});

OwnerDetailTreeViewController.$inject = [
  '$scope',
  '$q',
  '$http',
  '$state',
  'CLMContextLocations',
  'local.role.service',
  '$ngRedux',
];
