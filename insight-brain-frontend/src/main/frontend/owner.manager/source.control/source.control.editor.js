/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './source.control.editor.view.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SourceControlEditorController
};

function SourceControlEditorController(CLMContextLocations, OrganizationStore, ApplicationStore, $q, Messages,
                                       SameOwnerStateNavigationService, DeleteModalService, SourceControlService,
                                       $scope, ProductFeatures) {
  var vm = this;

  vm.ownerName = undefined;
  vm.ownerId = undefined;
  vm.ownerType = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.dirtySourceControl = undefined;
  vm.sourceControlEditorMask = undefined;
  vm.sourceControlEditor = undefined;
  vm.submitError = undefined;
  vm.loadError = undefined;
  vm.originalSourceControl = {};
  vm.providerTypes = SourceControlService.getProviderTypes();
  vm.providerTypesMap = SourceControlService.getProviderTypesMap();
  vm.isAccessTokenRequiredOnNode = isAccessTokenRequiredOnNode;
  vm.showAdvanced = undefined;
  vm.toggleShowAdvanced = toggleShowAdvanced;
  vm.shouldShowAccessTokenWarning = undefined;
  vm.canCollapseAdvanced = canCollapseAdvanced;
  vm.statusChecksInheritText = undefined;
  vm.enablePullRequestsInheritText = undefined;
  vm.baseBranchInheritText = undefined;
  vm.isPullRequestsSupported = isPullRequestsSupported;
  vm.getPullRequestsNotAvailableMessage = getPullRequestsNotAvailableMessage;
  vm.isProviderSpecifiedAndPullRequestsSupported = isProviderSpecifiedAndPullRequestsSupported;
  vm.isAutomationSupported = undefined;
  vm.isSourceControlSupported = undefined;

  vm.isDirty = isDirty;
  vm.save = save;
  vm.deleteSourceControl = deleteSourceControl;
  vm.doLoad = doLoad;
  vm.doLoad();

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });

  function doLoad() {
    vm.loadError = undefined;
    vm.loading = true;

    let ownerPromise;

    if (vm.isApp) {
      ownerPromise = ApplicationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'application';
    }
    else if (vm.isOrg) {
      ownerPromise = OrganizationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'organization';
    }

    if (ownerPromise !== undefined) {
      const promises = [
        ownerPromise,
        ProductFeatures.load()
      ];

      $q.all(promises).then(function(results) {
        vm.ownerName = results[0].name;
        vm.ownerId = results[0].id;
        let isNotificationsSupported = ProductFeatures.isAvailable('notifications');
        vm.isAutomationSupported = ProductFeatures.isAvailable('automation');
        vm.isSourceControlSupported = isNotificationsSupported || vm.isAutomationSupported;
        if (vm.isSourceControlSupported) {
          return getSourceControl();
        }
      }).catch(function(e) {
        vm.loadError = Messages.getHttpErrorMessage(e);
      }).finally(function() {
        vm.loading = false;
      });
    }
  }

  function getSourceControl() {
    return SourceControlService.getCompositeSourceControlRecord(vm.ownerType, vm.ownerId).then(function(result) {
      let compositeSourceControl = typeof result !== 'undefined' && result !== null ? result : {};
      vm.dirtySourceControl = compositeSourceControlToModel(compositeSourceControl);
      vm.dirtySourceControl.tokenInherit = vm.dirtySourceControl.tokenInherit && !isAccessTokenRequiredOnNode();
      vm.originalSourceControl = angular.copy(vm.dirtySourceControl);
      vm.shouldShowAccessTokenWarning = isAccessTokenRequiredOnNode() && vm.dirtySourceControl.token === null;
      vm.showAdvanced = !vm.isApp || !canCollapseAdvanced();
      vm.statusChecksInheritText = getInheritText(vm.dirtySourceControl.enableStatusChecksInheritFrom,
          vm.dirtySourceControl.enableStatusChecksInheritedValue ? 'Enabled' : 'Disabled');
      vm.pullRequestsInheritText = getInheritText(vm.dirtySourceControl.enablePullRequestsInheritFrom,
          vm.dirtySourceControl.enablePullRequestsInheritedValue ? 'Enabled' : 'Disabled');
      vm.baseBranchInheritText = getInheritText(vm.dirtySourceControl.baseBranchInheritFrom,
          vm.dirtySourceControl.baseBranchInheritedValue);
    });
  }

  function deleteSourceControl() {
    let message =
        `You are about to permanently remove Source Control configuration for ${vm.ownerType} \
         ${vm.ownerName}. This action cannot be undone.`;
    DeleteModalService.deleteCustom('Delete Source Control', message, 'Deleting', function() {
      return SourceControlService.deleteSourceControlRecord(vm.ownerType, vm.ownerId);
    }).then(function() {
      vm.dirtySourceControl = {};
      vm.originalSourceControl = {};
      vm.sourceControlEditor.$setPristine();
      SameOwnerStateNavigationService.goEdit('edit-source-control');
      doLoad();
    });
  }

  function save() {
    vm.submitError = undefined;
    let savePromise;
    let sourceControl = modelToSourceControl(vm.dirtySourceControl);

    if (vm.dirtySourceControl.id) {
      savePromise = SourceControlService.updateSourceControlRecord(vm.ownerType, vm.ownerId, sourceControl);
    }
    else {
      savePromise = SourceControlService.addSourceControlRecord(vm.ownerType, vm.ownerId, sourceControl);
    }

    vm.sourceControlEditorMask.wrap(savePromise).then(function() {
      doLoad();
    }).catch(function(e) {
      vm.submitError = Messages.getHttpErrorMessage(e);
    });
  }

  function isDirty() {
    let original = modelToSourceControl(vm.originalSourceControl);
    let dirty = modelToSourceControl(vm.dirtySourceControl);
    return (original !== dirty && !angular.equals(original, dirty))
        || vm.dirtySourceControl.baseBranchInherit !== vm.originalSourceControl.baseBranchInherit
        || vm.dirtySourceControl.tokenInherit !== vm.originalSourceControl.tokenInherit;
  }

  function compositeSourceControlToModel(compositeSourceControl) {
    let model = {};

    model.ownerId = compositeSourceControl.ownerId;
    model.id = compositeSourceControl.id;
    model.provider = compositeSourceControl.provider;
    model.repositoryUrl = compositeSourceControl.repositoryUrl;

    model.token = compositeSourceControl.token.value;
    model.tokenInherit = compositeSourceControl.token.value === null && !vm.isRootOrg;
    model.tokenInheritFrom = compositeSourceControl.token.parentName;

    model.baseBranch = getBaseBranchValue(compositeSourceControl.baseBranch.value);
    model.baseBranchInherit = compositeSourceControl.baseBranch.value === null && !vm.isRootOrg;
    model.baseBranchInheritFrom = compositeSourceControl.baseBranch.parentName;
    model.baseBranchInheritedValue = compositeSourceControl.baseBranch.parentValue;

    model.enablePullRequests = compositeSourceControl.enablePullRequests.value;
    model.enablePullRequestsInheritFrom =
        compositeSourceControl.enablePullRequests.parentName;
    model.enablePullRequestsInheritedValue =
        compositeSourceControl.enablePullRequests.parentValue;

    model.enableStatusChecks = compositeSourceControl.enableStatusChecks.value;
    model.enableStatusChecksInheritFrom =
        compositeSourceControl.enableStatusChecks.parentName;
    model.enableStatusChecksInheritedValue =
        compositeSourceControl.enableStatusChecks.parentValue;

    return model;
  }

  function modelToSourceControl(model) {
    let sourceControl = {};

    sourceControl.ownerId = model.ownerId;
    sourceControl.id = model.id;
    sourceControl.enablePullRequests = getPullRequestsEnabledFlagFromModel(model);
    sourceControl.enableStatusChecks = true;

    if (vm.isRootOrg) {
      sourceControl.provider = model.provider;
    }
    else if (vm.isApp) {
      sourceControl.repositoryUrl = model.repositoryUrl;
    }

    if (!model.tokenInherit || (vm.isRootOrg && model.token)) {
      sourceControl.token = model.token === '' ? null : model.token;
    }
    else {
      sourceControl.token = null;
    }

    if (!model.baseBranchInherit || (vm.isRootOrg && model.baseBranch)) {
      sourceControl.baseBranch = model.baseBranch === '' ? null : getBaseBranchValueFromModel(model);
    }
    else {
      sourceControl.baseBranch = null;
    }
    return sourceControl;
  }

  function isAccessTokenRequiredOnNode() {
    return vm.isApp && !vm.dirtySourceControl.tokenInheritFrom;
  }

  function toggleShowAdvanced() {
    vm.showAdvanced = !vm.showAdvanced || !vm.canCollapseAdvanced();
  }

  function canCollapseAdvanced() {
    return vm.isApp && (vm.dirtySourceControl.tokenInherit || vm.dirtySourceControl.token)
        && (vm.dirtySourceControl.baseBranchInherit || vm.dirtySourceControl.baseBranch);
  }

  function getBaseBranchValue(value) {
    if (!value && vm.isRootOrg) {
      return 'master';
    }
    else {
      return value;
    }
  }

  function getInheritText(parentName, parentValue) {
    if (parentName !== null) {
      return `Inherit from ${parentName} (${parentValue})`;
    }
    else {
      return 'Inherit (Not Configured)';
    }
  }

  function isPullRequestsSupported() {
    return (!vm.dirtySourceControl.provider || vm.dirtySourceControl.provider === 'github') && vm.isAutomationSupported;
  }

  function getPullRequestsNotAvailableMessage() {
    if (isPullRequestsSupported()) {
      return '';
    }

    return vm.isAutomationSupported ? 'This feature is not currently supported for ' +
        vm.providerTypesMap[vm.dirtySourceControl.provider] : 'This feature is not supported by your licence';
  }

  function getPullRequestsEnabledFlagFromModel(model) {
    if (!vm.isRootOrg || isPullRequestsSupported()) {
      return model.enablePullRequests;
    }

    return vm.originalSourceControl.enablePullRequests === null ? true : vm.originalSourceControl.enablePullRequests;
  }

  function getBaseBranchValueFromModel(model) {
    if (!vm.isRootOrg || isPullRequestsSupported()) {
      return model.baseBranch;
    }

    return vm.originalSourceControl.baseBranch === null ? 'master' : vm.originalSourceControl.baseBranch;
  }

  function isProviderSpecifiedAndPullRequestsSupported() {
    return vm.dirtySourceControl.provider && isPullRequestsSupported();
  }
}

SourceControlEditorController.$inject = [
  'CLMContextLocations', 'OrganizationStore', 'ApplicationStore', '$q', 'Messages', 'SameOwnerStateNavigationService',
  'DeleteModalService', 'SourceControlService', '$scope', 'ProductFeatures'
];
