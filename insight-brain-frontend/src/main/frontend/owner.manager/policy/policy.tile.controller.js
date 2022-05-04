/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSlice';
import { actions as propiertaryConfigActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSlice';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectMonitoredStageFromActionStages } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSelectors';
import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsLoading as selectPropietaryConfigIsLoading,
  selectPropietaryConfigInheritedMatchersCount,
  selectPropietaryConfigLocalMatchersCount,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

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
  vm.policiesByOwner = undefined;
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
    loadPropietaryConfig: propiertaryConfigActions.loadProprietaryConfig,
    loadActionStageTypes: stagesActions.loadActionStages,
    loadApplicablePoliciesByOwner: policyActions.loadApplicablePoliciesByOwner,
  })(vm);

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);

  function doLoad() {
    const promises = [
      vm.loadApplicablePoliciesByOwner(),
      vm.loadActionStageTypes(),
      vm.loadPropietaryConfig(),
      vm.loadApplicablePolicyMonitoring(),
    ];
    if (vm.isAppOrOrg) {
      promises.push(PolicyViolationGrandfatheringService.getGrandfathering());
    }

    $q.all(promises)
      .then(
        function (results) {
          vm.policiesByOwner = unwrapResult(results[0]).policiesByOwner;
          vm.actionStages = unwrapResult(results[1]).data;
          unwrapResult(results[2]);
          unwrapResult(results[3]);

          vm.policiesByOwner.forEach(function (policyOwner, index) {
            policyOwner.inherited = index > 0;
            policyOwner.policies.forEach(function (policy) {
              policy.enforcementAction = {};
              vm.actionStages.forEach(function (actionStage) {
                if (policy.actions[actionStage.stageTypeId]) {
                  policy.enforcementAction[actionStage.stageTypeId] = policy.actions[actionStage.stageTypeId];
                }
              });
            });
          });

          if (vm.isAppOrOrg) {
            vm.grandfatheringStatusMessage = PolicyViolationGrandfatheringService.getStatusMessage(results[4]);
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
  localProprietaryCount: selectPropietaryConfigLocalMatchersCount(state),
  inheritedProprietaryCount: selectPropietaryConfigInheritedMatchersCount(state),
  propietaryConfigIsloading: selectPropietaryConfigIsLoading(state),
  monitoredStage: selectMonitoredStageFromActionStages(state),
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
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
