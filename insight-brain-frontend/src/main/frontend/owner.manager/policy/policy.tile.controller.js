/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSlice';
import { actions as propiertaryConfigActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySlice';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectGrandfatheringStatusMessage,
  selectPoliciesByOwner,
  selectPolicyMonitoringActionStages,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringMonitoredStage,
  selectPolicyMonitoringOwnerName,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSelectors';
import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsLoading as selectPropietaryConfigIsLoading,
  selectPropietaryConfigInheritedMatchersCount,
  selectPropietaryConfigLocalMatchersCount,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySelectors';

export default function PolicyTileController(
  $scope,
  StageTypeStore,
  SameOwnerStateNavigationService,
  EventNameConstant,
  CLMContextLocations,
  PolicyViolationGrandfatheringService,
  $ngRedux
) {
  var vm = this;
  vm.isAppOrOrg = CLMContextLocations.isApplication() || CLMContextLocations.isOrganization();
  vm.ownerName = undefined;
  vm.policiesByOwner = undefined;
  vm.loadError = undefined;
  vm.actionStages = undefined;
  vm.monitoredStage = undefined;
  vm.localProprietaryCount = 0;
  vm.inheritedProprietaryCount = 0;
  vm.grandfatheringStatusMessage = undefined;
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.isMonitoringSupported = undefined;
  vm.isGrandfatheringSupported = undefined;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.editPolicy = editPolicy;
  vm.doLoad = doLoad;

  vm.$onInit = function () {
    vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
      loadApplicablePolicyMonitoring: actions.loadApplicablePolicyMonitoring,
      loadPropietaryConfig: propiertaryConfigActions.loadProprietaryConfig,
    })(vm);
    vm.doLoad();
  };

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on('policy.imported', doLoad);
  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    const promises = [StageTypeStore.getActionStages()];
    if (vm.isAppOrOrg) {
      promises.push(PolicyViolationGrandfatheringService.getGrandfathering());
    }

    vm.loadPropietaryConfig();
    vm.loadApplicablePolicyMonitoring({
      promises: () =>
        Promise.all(promises).then(([actionStages, grandfathering]) => ({
          actionStages,
          grandfathering,
        })),
    });
  }

  function editPolicy(policyId) {
    SameOwnerStateNavigationService.goEdit('policy', { policyId: policyId });
  }

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
  }

  function isEnforcementSupportedForStage(stage) {
    return (vm.isFirewallSupported && stage === 'proxy') || vm.isEnforcementSupported;
  }
}

export const mapStateToThis = (state) => ({
  ownerProperties: selectOwnerProperties(state),
  ownerName: selectPolicyMonitoringOwnerName(state),
  policiesByOwner: selectPoliciesByOwner(state),
  grandfatheringStatusMessage: selectGrandfatheringStatusMessage(state),
  localProprietaryCount: selectPropietaryConfigLocalMatchersCount(state),
  inheritedProprietaryCount: selectPropietaryConfigInheritedMatchersCount(state),
  propietaryConfigIsloading: selectPropietaryConfigIsLoading(state),
  monitoredStage: selectPolicyMonitoringMonitoredStage(state),
  loadError: selectPolicyMonitoringLoadError(state),
  actionStages: selectPolicyMonitoringActionStages(state),
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
});

PolicyTileController.$inject = [
  '$scope',
  'StageTypeStore',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  'CLMContextLocations',
  'policyViolationGrandfatheringService',
  '$ngRedux',
];
