/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/OrgsAndPolicies/сontinuousMonitoring/policyMonitoringSlice';
import { actions as proprietaryConfigActions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import {
  selectIsMonitoringSupported,
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectMonitoredStageFromActionStages } from 'MainRoot/OrgsAndPolicies/сontinuousMonitoring/policyMonitoringSelectors';
import {
  selectSelectedOwner,
  selectPoliciesByOwner,
  selectSelectedOwnerName,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsLoading as selectProprietaryConfigIsLoading } from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { getActionsOverride } from 'MainRoot/OrgsAndPolicies/utility/util';

export default function PolicyTileController(
  $scope,
  $q,
  SameOwnerStateNavigationService,
  EventNameConstant,
  CLMContextLocations,
  $ngRedux
) {
  var vm = this;
  vm.isAppOrOrg = CLMContextLocations.isApplication() || CLMContextLocations.isOrganization();
  vm.loadError = undefined;
  vm.actionStages = undefined;
  vm.monitoredStage = undefined;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.editPolicy = editPolicy;
  vm.doLoad = doLoad;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplicablePolicyMonitoring: actions.loadApplicablePolicyMonitoring,
    loadProprietaryConfig: proprietaryConfigActions.loadProprietaryConfig,
    loadActionStageTypes: stagesActions.loadActionStages,
    setPoliciesByOwner: rootActions.setPoliciesByOwner,
    loadApplicablePoliciesByOwner: rootActions.loadApplicablePoliciesByOwner,
  })(vm);

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on(EventNameConstant.POLICY_IMPORTED, () => {
    vm.loadApplicablePoliciesByOwner()
      .then(unwrapResult)
      .then(vm.doLoad)
      .catch((error) => {
        vm.loadError = error;
      });
  });
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);

  function doLoad() {
    vm.loading = true;
    const promises = [
      vm.loadApplicablePoliciesByOwner(),
      vm.loadActionStageTypes(),
      vm.loadProprietaryConfig(),
      vm.loadApplicablePolicyMonitoring(),
    ];

    $q.all(promises)
      .then(function (results) {
        vm.actionStages = unwrapResult(results[1]).data;
        const ownerIds = vm.policiesByOwner?.map(prop('ownerId'));

        const updatedPoliciesByOwner = vm.policiesByOwner?.map(function (policyOwner, index) {
          const policies = policyOwner.policies.map((policy) => {
            const actionsOverrideInfo = getActionsOverride(ownerIds, policy);
            const actions = actionsOverrideInfo?.actionsOverride || policy.actions;

            const enforcementAction = {};
            vm.actionStages?.forEach(function (actionStage) {
              if (actions[actionStage.stageTypeId]) {
                enforcementAction[actionStage.stageTypeId] = actions[actionStage.stageTypeId];
              }
            });

            return {
              ...policy,
              hasLocalActionsOverrides: actionsOverrideInfo?.isCurrentOwnerOverride,
              enforcementAction,
            };
          });

          return {
            ...policyOwner,
            inherited: index > 0,
            policies,
          };
        });

        vm.setPoliciesByOwner(updatedPoliciesByOwner);
      })
      .catch((error) => {
        vm.loadError = error;
      })
      .finally(() => {
        vm.loading = false;
      });
  }

  function editPolicy(policyId) {
    SameOwnerStateNavigationService.goEdit('policy', { policyId: policyId });
  }

  function isEnforcementSupportedForStage(stage) {
    return (vm.isFirewallSupported && stage === 'proxy') || vm.isEnforcementSupported;
  }
}

export const mapStateToThis = (state) => ({
  owner: selectSelectedOwner(state),
  ownerName: selectSelectedOwnerName(state),
  proprietaryConfigIsLoading: selectProprietaryConfigIsLoading(state),
  monitoredStage: selectMonitoredStageFromActionStages(state),
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  policiesByOwner: selectPoliciesByOwner(state),
});

PolicyTileController.$inject = [
  '$scope',
  '$q',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  'CLMContextLocations',
  '$ngRedux',
];
