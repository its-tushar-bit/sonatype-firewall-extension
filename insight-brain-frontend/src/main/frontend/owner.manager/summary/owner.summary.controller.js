/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { propEq, find } from 'ramda';
import { unwrapResult } from '@reduxjs/toolkit';

import { actions as applicationsActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import {
  selectIsInnerSourceRepositorySupported,
  selectIsArtifactoryRepositorySupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectDashboardStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { selectRepositoryUrl, selectScmProviderIcon } from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import { selectSelectedOwner, selectPoliciesByOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { selectImportPoliciesSlice } from 'MainRoot/OrgsAndPolicies/importPoliciesModal/importPoliciesSelectors';
import { selectMoveApplicationSlice } from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSelectors';
import { selectContactSlice } from 'MainRoot/OrgsAndPolicies/selectContactModal/selectContactModalSelectors';
import { selectEvaluateApplicationSlice } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSelectors';

export default function OwnerSummaryController(
  $state,
  $scope,
  $q,
  $http,
  CLMLocations,
  CLMContextLocations,
  ownerConstant,
  EventNameConstant,
  $ngRedux
) {
  var vm = this;

  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.isRepositoryContainer = CLMContextLocations.isRepositoryContainer();
  vm.stages = undefined;
  vm.doLoad = doLoad;

  var siblings,
    stateIdField = vm.isRepositoryContainer
      ? 'repositoryContainerId'
      : vm.isApp
      ? 'applicationPublicId'
      : 'organizationId',
    type = vm.isRepositoryContainer
      ? ownerConstant.REPOSITORY_CONTAINER_TYPE
      : vm.isApp
      ? ownerConstant.APPLICATION_TYPE
      : ownerConstant.ORGANIZATION_TYPE,
    id = $state.params[stateIdField];

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadDashboardStageTypes: stagesActions.loadDashboardStages,
    setSelectedOwner: rootActions.setSelectedOwner,
    setSelectedOwnerContact: rootActions.setSelectedOwnerContact,
    loadApplications: applicationsActions.loadApplications,
    loadOrganizations: organizationsActions.loadOrganizations,
    loadApplicablePoliciesByOwner: rootActions.loadApplicablePoliciesByOwner,
    setLoading: ownerSummaryActions.setLoading,
    setLoadError: ownerSummaryActions.setLoadError,
  })(vm);

  vm.doLoad();

  if (vm.isApp) {
    $scope.$watch('vm.scanId', (currentValue) => {
      if (currentValue) {
        $http.get(CLMLocations.getApplicationSummaryUrl(id)).then(
          (result) => {
            vm.applicationSummary = result.data;
          },
          (error) => {
            vm.setLoadError(error);
          }
        );
      }
    });

    $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
    $scope.$on(EventNameConstant.OWNER_UPDATED, doLoad);
    $scope.$watch('vm.isShowSuccessMoveAppModal', (currentValue, oldValue) => {
      // triggers doLoad func only when modal closes, i.e. mask going from true to null
      if (!currentValue && oldValue) {
        vm.doLoad();
      }
    });

    $scope.$watch('vm.isShowSuccessSelectContactModal', (currentValue, oldValue) => {
      // triggers doLoad func only when modal closes, i.e. mask going from true to null
      if (!currentValue && oldValue) {
        vm.doLoad();
      }
    });
  }

  $scope.$watch('vm.isShowSuccessImportPoliciesModal', (currentValue, oldValue) => {
    // triggers doLoad func only when modal closes, i.e. mask going from true to null
    if (!currentValue && oldValue) {
      vm.doLoad();
    }
  });

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  function doLoad() {
    vm.setLoading(true);
    vm.setLoadError(null);

    const promises = [
      vm.isApp ? vm.loadApplications(true) : vm.loadOrganizations(true),
      vm.loadApplicablePoliciesByOwner(),
    ];

    if (vm.isApp) {
      promises.push(vm.loadDashboardStageTypes());
      promises.push($http.get(CLMLocations.getApplicationSummaryUrl(id)));
    }

    $q.all(promises)
      .then((results) => {
        siblings = unwrapResult(results[0]);

        const entityId = CLMContextLocations.getEntityId();
        const owner = find(propEq(vm.isApp ? 'publicId' : 'id', entityId))(siblings);
        if (!owner) {
          throw `Could not find an ${type} with ID ${entityId}.`;
        }
        vm.setSelectedOwner(owner);

        if (vm.isApp) {
          vm.applicationSummary = results[3].data;
          vm.setSelectedOwnerContact(vm.applicationSummary.contact);
        }
      })
      .catch((error) => {
        vm.setLoadError(error);
      })
      .finally(() => {
        vm.setLoading(false);
      });
  }
}

const mapStateToThis = (state) => ({
  stages: selectDashboardStageTypes(state),
  isInnerSourceRepositorySupported: selectIsInnerSourceRepositorySupported(state),
  isArtifactoryRepositorySupported: selectIsArtifactoryRepositorySupported(state),
  owner: selectSelectedOwner(state),
  repositoryUrl: selectRepositoryUrl(state),
  scmProviderIcon: selectScmProviderIcon(state),
  policiesByOwner: selectPoliciesByOwner(state),
  loading: selectLoading(state),
  loadError: selectLoadError(state),
  isShowSuccessImportPoliciesModal: selectImportPoliciesSlice(state).submitMaskState,
  isShowSuccessMoveAppModal: selectMoveApplicationSlice(state).isShowSuccessModal,
  isShowSuccessSelectContactModal: selectContactSlice(state).submitMaskState,
  scanId: selectEvaluateApplicationSlice(state).evaluationStatus.scanId,
});

OwnerSummaryController.$inject = [
  '$state',
  '$scope',
  '$q',
  '$http',
  'CLMLocations',
  'CLMContextLocations',
  'owner.constant',
  'event.name.constant',
  '$ngRedux',
];
