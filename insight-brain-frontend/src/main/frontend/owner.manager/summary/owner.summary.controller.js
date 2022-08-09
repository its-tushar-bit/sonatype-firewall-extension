/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { propEq, find } from 'ramda';
import { unwrapResult } from '@reduxjs/toolkit';

import { actions as deleteOwnerActions } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSlice';
import { actions as applicationsActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import {
  selectIsGrandfatheringSupported,
  selectIsInnerSourceRepositorySupported,
  selectIsArtifactoryRepositorySupported,
  selectIsEvaluateApplicationAvailable,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectDashboardStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { selectRepositoryUrl, selectScmProviderIcon } from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import { selectSelectedOwner, selectPoliciesByOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import {
  selectCalculatedEnabled,
  selectGrandfatheringStatusMessage,
} from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSelectors';

export default function OwnerSummaryController(
  $state,
  $scope,
  $q,
  $http,
  $window,
  OwnerEditor,
  CLMLocations,
  CLMContextLocations,
  SelectApplicationContactService,
  EvaluateApplicationModalService,
  ImportPolicyModalService,
  ownerConstant,
  MoveApplicationModal,
  EventNameConstant,
  ChangeApplicationIdService,
  PermissionService,
  RevokeGrandfatheringModalService,
  GrandfatherModalService,
  $ngRedux
) {
  var vm = this;

  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.stages = undefined;
  vm.doLoad = doLoad;
  vm.edit = edit;
  vm.moveApplication = moveApplication;
  vm.evaluateApp = evaluateApp;
  vm.importPolicy = importPolicy;
  vm.deleteOwner = deleteOwner;
  vm.grandfather = grandfather;
  vm.revokeGrandfathering = revokeGrandfathering;
  vm.getShortTypeName = getShortTypeName;
  vm.getResourceTypeName = getResourceTypeName;
  vm.openReport = openReport;
  vm.selectContact = selectContact;
  vm.changeApplicationId = changeApplicationId;
  vm.hasPermissionToChangeAppId = undefined;
  vm.hasPermissionToEvaluateApp = undefined;
  vm.getDisabledGrandfatherTooltipMessage = getDisabledGrandfatherTooltipMessage;
  vm.getDisabledEvaluateTooltipMessage = getDisabledEvaluateTooltipMessage;

  var siblings,
    stateIdField = vm.isApp ? 'applicationPublicId' : 'organizationId',
    type = vm.isApp ? ownerConstant.APPLICATION_TYPE : ownerConstant.ORGANIZATION_TYPE,
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
    openDeleteModal: deleteOwnerActions.openModal,
  })(vm);

  vm.doLoad();

  if (vm.isApp) {
    $scope.$on('reload.app.report.data', function () {
      $http.get(CLMLocations.getApplicationSummaryUrl(id)).then(
        function (result) {
          vm.applicationSummary = result.data;
        },
        function (error) {
          vm.setLoadError(error);
        }
      );
    });

    $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
    $scope.$on(EventNameConstant.OWNER_UPDATED, doLoad);
  }

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

          getAppChangePermissions();
          getAppEvaluatePermissions();
        }
      })
      .catch((error) => {
        vm.setLoadError(error);
      })
      .finally(() => {
        vm.setLoading(false);
      });
  }

  function getAppChangePermissions() {
    PermissionService.isContextAuthorized(['WRITE'], 'application', vm.owner.id).then(function (hasPermission) {
      vm.hasPermissionToChangeAppId = hasPermission;
    });
  }

  function getAppEvaluatePermissions() {
    PermissionService.isContextAuthorized(['EVALUATE_APPLICATION'], 'application', vm.owner.id).then(function (
      hasPermission
    ) {
      vm.hasPermissionToEvaluateApp = hasPermission;
    });
  }

  function edit() {
    OwnerEditor.open(vm.owner, type, siblings);
  }

  function moveApplication() {
    MoveApplicationModal.open(vm.owner);
  }

  function evaluateApp() {
    if (vm.hasPermissionToEvaluateApp && vm.isEvaluateApplicationAvailable) {
      EvaluateApplicationModalService.open(vm.owner);
    }
  }

  function importPolicy() {
    ImportPolicyModalService.open();
  }

  function selectContact(owner) {
    SelectApplicationContactService.open(owner);
  }

  function changeApplicationId() {
    if (vm.hasPermissionToChangeAppId) {
      ChangeApplicationIdService.open(vm.owner, siblings);
    }
  }

  function deleteOwner() {
    vm.openDeleteModal();
  }

  function revokeGrandfathering() {
    if (vm.isGrandfatheringSupported) {
      RevokeGrandfatheringModalService.open(vm.owner);
    }
  }

  function grandfather() {
    if (vm.isGrandfatheringEnabled && vm.isGrandfatheringSupported) {
      GrandfatherModalService.open(vm.owner);
    }
  }

  function getShortTypeName() {
    return vm.isApp ? 'App' : 'Org';
  }

  function getResourceTypeName() {
    return vm.isApp ? 'Application' : 'Organization';
  }

  function openReport(stage) {
    if (vm.applicationSummary.policyEvaluations[stage.stageTypeId]) {
      $window.open(
        $state.href('applicationReport.policy', {
          publicId: vm.applicationSummary.publicId,
          scanId: vm.applicationSummary.policyEvaluations[stage.stageTypeId].scanId,
        }),
        '_blank'
      );
    }
  }

  function getDisabledGrandfatherTooltipMessage() {
    if (!vm.isGrandfatheringSupported) {
      return 'Policy Violation Grandfathering is not supported by your license';
    } else if (!vm.isGrandfatheringEnabled) {
      return 'Grandfathering is not enabled for this application.';
    }

    return undefined;
  }

  function getDisabledEvaluateTooltipMessage() {
    if (!vm.hasPermissionToEvaluateApp && vm.isEvaluateApplicationAvailable) {
      return 'Insufficient permissions to evaluate application';
    } else if (!vm.isEvaluateApplicationAvailable) {
      return 'Evaluate application is not supported by your license.';
    }

    return undefined;
  }
}

const mapStateToThis = (state) => ({
  stages: selectDashboardStageTypes(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  isEvaluateApplicationAvailable: selectIsEvaluateApplicationAvailable(state),
  isInnerSourceRepositorySupported: selectIsInnerSourceRepositorySupported(state),
  isArtifactoryRepositorySupported: selectIsArtifactoryRepositorySupported(state),
  owner: selectSelectedOwner(state),
  repositoryUrl: selectRepositoryUrl(state),
  scmProviderIcon: selectScmProviderIcon(state),
  policiesByOwner: selectPoliciesByOwner(state),
  loading: selectLoading(state),
  loadError: selectLoadError(state),
  isGrandfatheringEnabled: selectCalculatedEnabled(state),
  grandfatheringStatusMessage: selectGrandfatheringStatusMessage(state),
});

OwnerSummaryController.$inject = [
  '$state',
  '$scope',
  '$q',
  '$http',
  '$window',
  'OwnerEditorService',
  'CLMLocations',
  'CLMContextLocations',
  'SelectApplicationContactService',
  'evaluate.application.modal.service',
  'import.policy.modal.service',
  'owner.constant',
  'move.application.modal.service',
  'event.name.constant',
  'change.application.id.service',
  'PermissionService',
  'RevokeGrandfatheringModalService',
  'GrandfatherModalService',
  '$ngRedux',
];
