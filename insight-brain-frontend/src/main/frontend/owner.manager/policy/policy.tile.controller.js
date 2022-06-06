/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import { actions as proprietaryConfigActions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectMonitoredStageFromActionStages } from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';
import {
  selectOwnerProperties,
  selectPoliciesByOwner,
  selectSelectedOwnerName,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsLoading as selectProprietaryConfigIsLoading,
  selectProprietaryConfigInheritedMatchersCount,
  selectProprietaryConfigLocalMatchersCount,
} from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

export default function PolicyTileController(
  $scope,
  $q,
  SameOwnerStateNavigationService,
  EventNameConstant,
  CLMContextLocations,
  PolicyViolationGrandfatheringService,
  $ngRedux
) {
  var vm = this;
  vm.isAppOrOrg = CLMContextLocations.isApplication() || CLMContextLocations.isOrganization();
  vm.loadError = undefined;
  vm.actionStages = undefined;
  vm.monitoredStage = undefined;
  vm.localProprietaryCount = 0;
  vm.inheritedProprietaryCount = 0;
  vm.grandfatheringStatusMessage = undefined;
  vm.isRootOrg = CLMContextLocations.isRootOrg();
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
    const promises = [vm.loadActionStageTypes(), vm.loadProprietaryConfig(), vm.loadApplicablePolicyMonitoring()];
    if (vm.isAppOrOrg) {
      promises.push(PolicyViolationGrandfatheringService.getGrandfathering());
    }

    $q.all(promises)
      .then(
        function (results) {
          vm.actionStages = unwrapResult(results[0]).data;

          const updatedPoliciesByOwner = vm.policiesByOwner?.map(function (policyOwner, index) {
            const policies = policyOwner.policies.map((policy) => {
              const enforcementAction = {};
              vm.actionStages.forEach(function (actionStage) {
                if (policy.actions[actionStage.stageTypeId]) {
                  enforcementAction[actionStage.stageTypeId] = policy.actions[actionStage.stageTypeId];
                }
              });
              return {
                ...policy,
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

          if (vm.isAppOrOrg) {
            vm.grandfatheringStatusMessage = PolicyViolationGrandfatheringService.getStatusMessage(results[3]);
          }
        },
        function (error) {
          vm.loadError = error;
        }
      )
      .catch((error) => {
        vm.loadError = error;
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
  ownerProperties: selectOwnerProperties(state),
  ownerName: selectSelectedOwnerName(state),
  localProprietaryCount: selectProprietaryConfigLocalMatchersCount(state),
  inheritedProprietaryCount: selectProprietaryConfigInheritedMatchersCount(state),
  proprietaryConfigIsLoading: selectProprietaryConfigIsLoading(state),
  monitoredStage: selectMonitoredStageFromActionStages(state),
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  policiesByOwner: selectPoliciesByOwner(state),
});

PolicyTileController.$inject = [
  '$scope',
  '$q',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  'CLMContextLocations',
  'policyViolationGrandfatheringService',
  '$ngRedux',
];
