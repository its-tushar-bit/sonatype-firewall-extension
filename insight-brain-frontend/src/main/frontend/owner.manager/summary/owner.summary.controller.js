/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function OwnerSummaryController(
  $state,
  $scope,
  $rootScope,
  $q,
  $http,
  $window,
  OwnerEditor,
  ApplicationStore,
  OrganizationStore,
  CLMLocations,
  CLMContextLocations,
  StageTypeStore,
  DeleteModalService,
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
  PolicyViolationGrandfatheringService,
  ProductFeatures,
  SourceControlService
) {
  var vm = this;

  vm.error = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.owner = undefined;
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
  vm.goToParentView = goToParentView;
  vm.selectContact = selectContact;
  vm.changeApplicationId = changeApplicationId;
  vm.hasPermissionToChangeAppId = undefined;
  vm.hasPermissionToEvaluateApp = undefined;
  vm.isGrandfatheringEnabled = undefined;
  vm.isGrandfatheringSupported = undefined;
  vm.getDisabledGrandfatherTooltipMessage = getDisabledGrandfatherTooltipMessage;
  vm.getDisabledEvaluateTooltipMessage = getDisabledEvaluateTooltipMessage;
  vm.isEvaluateApplicationAvailable = undefined;
  vm.repositoryUrl = undefined;
  vm.scmProvider = undefined;
  vm.isInnerSourceRepositorySupported = undefined;

  var siblings,
    stateIdField = vm.isApp ? 'applicationPublicId' : 'organizationId',
    type = vm.isApp ? ownerConstant.APPLICATION_TYPE : ownerConstant.ORGANIZATION_TYPE,
    id = $state.params[stateIdField];

  vm.doLoad();

  if (vm.isApp) {
    $scope.$on('reload.app.report.data', function () {
      $http.get(CLMLocations.getApplicationSummaryUrl(id)).then(
        function (result) {
          vm.applicationSummary = result.data;
        },
        function (error) {
          vm.error = error;
        }
      );
    });

    $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
  }

  function doLoad() {
    var store = vm.isApp ? ApplicationStore : OrganizationStore,
      promises = [store[vm.error ? 'refresh' : 'get'](), store.getById(id), ProductFeatures.load()];

    if (vm.isApp) {
      promises.push(StageTypeStore.getDashboardStages());
      promises.push($http.get(CLMLocations.getApplicationSummaryUrl(id)));
      promises.push(PolicyViolationGrandfatheringService.getGrandfathering());
      promises.push(ApplicationStore.getById(CLMContextLocations.getEntityId()));
    }

    $q.all(promises).then(
      function (results) {
        siblings = results[0];
        vm.owner = results[1];
        vm.isGrandfatheringSupported = ProductFeatures.isAvailable('policy-grandfathering');
        vm.isEvaluateApplicationAvailable = ProductFeatures.isEvaluateApplicationAvailable();
        vm.isInnerSourceRepositorySupported = ProductFeatures.isAvailable('inner-source-repository-integration');

        if (vm.isApp) {
          vm.stages = results[3];
          vm.applicationSummary = results[4].data;
          vm.owner.contact = vm.applicationSummary.contact;
          vm.isGrandfatheringEnabled = results[5].calculatedEnabled;
          getAppChangePermissions();
          getAppEvaluatePermissions();
          getSourceControl(results[6].id);
        }
      },
      function (error) {
        vm.error = error;
      }
    );

    delete vm.error;
  }

  function getSourceControl(ownerInternalId) {
    return SourceControlService.getCompositeSourceControlRecord('application', ownerInternalId).then(function (result) {
      if (result && result.provider) {
        vm.repositoryUrl = result.repositoryUrl;
        vm.scmProviderIcon = result.provider.value ? result.provider.value : result.provider.parentValue;
        if (vm.scmProviderIcon === 'azure') {
          // no Font Awesome icon for Azure, use Microsoft instead once FA v5 is available (eg: React migration)
          // see: https://github.com/FortAwesome/Font-Awesome/issues/14058
          vm.scmProviderIcon = 'git';
        }
      }
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
    DeleteModalService.deleteResource(vm.getResourceTypeName(), vm.owner.name, vm.owner).then(function () {
      $rootScope.$broadcast('owner.deleted', vm.owner, type);
      vm.goToParentView();
    });
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

  function goToParentView() {
    if (!vm.isApp) {
      $state.go('management.view.organization', {
        organizationId: vm.owner.parentOrganizationId,
      });
    } else {
      $state.go('management.view.organization', {
        organizationId: vm.owner.organizationId,
      });
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

OwnerSummaryController.$inject = [
  '$state',
  '$scope',
  '$rootScope',
  '$q',
  '$http',
  '$window',
  'OwnerEditorService',
  'ApplicationStore',
  'OrganizationStore',
  'CLMLocations',
  'CLMContextLocations',
  'StageTypeStore',
  'DeleteModalService',
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
  'policyViolationGrandfatheringService',
  'ProductFeatures',
  'SourceControlService',
];
