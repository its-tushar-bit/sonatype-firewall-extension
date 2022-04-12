/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import {
  selectIsSourceControlForSourceTileSupported,
  selectIsSourceControlSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './source.control.editor.view.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SourceControlEditorController,
};

const ROOT_ORG_NAME = 'Root Organization';

const PROVIDERS_WITH_USERNAME = ['azure', 'bitbucket'];

function SourceControlEditorController(
  CLMContextLocations,
  OrganizationStore,
  ApplicationStore,
  $q,
  Messages,
  SameOwnerStateNavigationService,
  DeleteModalService,
  SourceControlService,
  $scope,
  UpdateSourceControlModalService,
  $ngRedux
) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
  })(vm);

  vm.ownerName = undefined;
  vm.ownerId = undefined;
  vm.ownerType = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();

  // source control
  vm.originalSourceControl = {};
  vm.dirtySourceControl = undefined;

  vm.sourceControlEditorMask = undefined;
  vm.sourceControlEditor = undefined;
  vm.submitError = undefined;
  vm.loadError = undefined;
  vm.showAdvanced = undefined;
  vm.toggleShowAdvanced = toggleShowAdvanced;
  vm.canCollapseAdvanced = canCollapseAdvanced;

  // features
  vm.isAutomationSupported = undefined;
  vm.isSourceControlSupported = undefined;

  // provider
  vm.providerTypes = SourceControlService.getProviderTypes();
  vm.providerTypesMap = SourceControlService.getProviderTypesMap();
  vm.providerInheritText = undefined;
  vm.effectiveProvider = effectiveProvider;

  // user
  vm.isUsernameRequiredOnNode = isUsernameRequiredOnNode;
  vm.usernameInheritText = undefined;
  vm.providerNeedsUsername = providerNeedsUsername;

  // default branch
  vm.baseBranchInheritText = undefined;

  // token
  vm.isAccessTokenRequiredOnNode = isAccessTokenRequiredOnNode;
  vm.shouldShowAccessTokenWarning = undefined;
  vm.effectiveTokenInheritFrom = effectiveTokenInheritFrom;

  // url
  vm.isSshUrl = isSshUrl;

  // PR commenting
  vm.pullRequestCommentingEnabledInheritText = undefined;
  vm.isPullRequestCommentingSupported = isPullRequestCommentingSupported;
  vm.getPullRequestCommentingNotAvailableMessage = getPullRequestCommentingNotAvailableMessage;
  vm.isProviderSpecifiedAndPullRequestCommentingSupported = isProviderSpecifiedAndPullRequestCommentingSupported;

  // remediation PRs
  vm.remediationPullRequestsEnabledInheritText = undefined;
  vm.arePullRequestsSupported = arePullRequestsSupported;
  vm.getPullRequestsNotAvailableMessage = getPullRequestsNotAvailableMessage;
  vm.isProviderSpecifiedAndPullRequestsSupported = isProviderSpecifiedAndPullRequestsSupported;
  vm.providersSupportingPullRequests = ['azure', 'bitbucket', 'github', 'gitlab'];

  // source scans/evaluations
  vm.sourceControlEvaluationsEnabledInheritText = undefined;
  vm.areSourceControlEvaluationsSupported = areSourceControlEvaluationsSupported;
  vm.getSourceControlEvaluationsNotAvailableMessage = getSourceControlEvaluationsNotAvailableMessage;
  vm.isProviderSpecifiedAndSourceControlEvaluationsSupported = isProviderSpecifiedAndSourceControlEvaluationsSupported;

  // ssh
  vm.sshEnabledInheritText = undefined;

  // status checks
  vm.statusChecksInheritText = undefined;

  // function reference to initiate the SCM Configuration validation
  vm.validateScmConfig = validateScmConfig;

  // result object of the SCM validation
  vm.scmConfigValidationResult = undefined;

  // flag to indicate SCM testing is in progress
  vm.scmConfigValidationInProgress = false;

  // helper function to generate the display classes
  vm.getScmValidationClass = getScmValidationClass;

  // pull request metrics associated with application
  vm.sourceControlMetrics = undefined;
  vm.effectiveProvider = effectiveProvider;
  vm.effectiveTokenInheritFrom = effectiveTokenInheritFrom;
  vm.providerNeedsUsername = providerNeedsUsername;
  vm.sshEnabled = sshEnabled;

  /**
   * Matches any absolute HTTP(S) as per RFC 3986
   */
  vm.repoCloneUrl = /(http[s]?:\/\/[^?#\s]+)/;

  vm.isDirty = isDirty;
  vm.save = save;
  vm.deleteSourceControl = deleteSourceControl;
  vm.doLoad = doLoad;
  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  function doLoad() {
    vm.loadError = undefined;
    vm.loading = true;

    let ownerPromise;

    if (vm.isApp) {
      ownerPromise = ApplicationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'application';
    } else if (vm.isOrg) {
      ownerPromise = OrganizationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'organization';
    }

    if (ownerPromise !== undefined) {
      const promises = [ownerPromise, vm.loadProductFeatures()];

      $q.all(promises)
        .then(function (results) {
          unwrapResult(results[1]);
          vm.ownerName = results[0].name;
          vm.ownerId = results[0].id;

          if (vm.isSourceControlSupported) {
            return getSourceControl();
          }
        })
        .catch(function (e) {
          vm.loadError = Messages.getHttpErrorMessage(e);
        })
        .finally(function () {
          vm.loading = false;
        });
    }
  }

  function getSourceControl() {
    var promises = [
      SourceControlService.getCompositeSourceControlRecord(vm.ownerType, vm.ownerId),
      SourceControlService.getSourceControlMetrics(vm.ownerType, vm.ownerId),
    ];
    return $q.all(promises).then(function (result) {
      let compositeSourceControl = typeof result[0] !== 'undefined' && result[0] !== null ? result[0] : {};
      vm.dirtySourceControl = compositeSourceControlToModel(compositeSourceControl);
      // set this value so that it can be used for intermediate calculations
      vm.originalSourceControl = angular.copy(vm.dirtySourceControl);
      vm.dirtySourceControl.usernameInherit =
        vm.dirtySourceControl.usernameInherit && !isUsernameRequiredOnNode() && providerNeedsUsername();
      vm.dirtySourceControl.credentialsInherit = vm.dirtySourceControl.usernameInherit && !isUsernameRequiredOnNode();
      vm.usernameInheritText = getInheritText(
        vm.dirtySourceControl.usernameInheritFrom,
        vm.dirtySourceControl.usernameInheritedValue
      );

      // force the xxxInherit properties to be 'false' if these values are missing in the hierarchy
      // but are required at this level
      vm.dirtySourceControl.tokenInherit = vm.dirtySourceControl.tokenInherit && !isAccessTokenRequiredOnNode();
      vm.dirtySourceControl.providerInherit = vm.dirtySourceControl.providerInherit && !isProviderRequiredOnNode();

      vm.originalSourceControl = angular.copy(vm.dirtySourceControl);
      vm.shouldShowAccessTokenWarning = isAccessTokenRequiredOnNode() && vm.dirtySourceControl.token === null;
      vm.showAdvanced = !vm.isApp || !canCollapseAdvanced();
      vm.providerInheritText = getInheritText(
        vm.dirtySourceControl.providerInheritFrom,
        vm.providerTypesMap[vm.dirtySourceControl.providerInheritValue]
      );
      vm.pullRequestCommentingEnabledInheritText = getEnabledDisabledInheritText(
        vm.dirtySourceControl.pullRequestCommentingEnabledInheritFrom,
        vm.dirtySourceControl.pullRequestCommentingEnabledInheritedValue
      );
      vm.statusChecksInheritText = getEnabledDisabledInheritText(
        vm.dirtySourceControl.statusChecksEnabledInheritFrom,
        vm.dirtySourceControl.statusChecksEnabledInheritedValue
      );
      vm.remediationPullRequestsEnabledInheritText = getEnabledDisabledInheritText(
        vm.dirtySourceControl.remediationPullRequestsEnabledInheritFrom,
        vm.dirtySourceControl.remediationPullRequestsEnabledInheritedValue
      );
      vm.sourceControlEvaluationsEnabledInheritText = getEnabledDisabledInheritText(
        vm.dirtySourceControl.sourceControlEvaluationsEnabledInheritFrom,
        vm.dirtySourceControl.sourceControlEvaluationsEnabledInheritedValue
      );
      vm.baseBranchInheritText = getInheritText(
        vm.dirtySourceControl.baseBranchInheritFrom,
        vm.dirtySourceControl.baseBranchInheritedValue
      );
      vm.sshEnabledInheritText = getEnabledDisabledInheritText(
        vm.dirtySourceControl.sshEnabledInheritFrom,
        vm.dirtySourceControl.sshEnabledInheritedValue
      );

      vm.sourceControlMetrics = result[1];
    });
  }

  /**
   * Perform the source control configuration test
   */
  function validateScmConfig() {
    vm.scmConfigValidationResult = undefined;
    vm.scmConfigValidationInProgress = true;
    return SourceControlService.validateCompositeSCMConfig(vm.ownerType, vm.ownerId).then(function (result) {
      vm.scmConfigValidationResult = result;
      vm.scmConfigValidationInProgress = false;
    });
  }

  function getScmValidationClass(result) {
    if (!result) {
      return 'fa-question-circle warn';
    }
    if (!result.valid) {
      return 'fa-exclamation-triangle warn';
    }
    return 'fa-check-circle text-success';
  }

  function deleteSourceControl() {
    let message = `You are about to reset the Source Control configuration for ${vm.ownerType} \
         ${vm.ownerName}. This action cannot be undone.`;
    DeleteModalService.deleteCustom('Reset Source Control', message, 'Resetting', function () {
      return SourceControlService.deleteSourceControlRecord(vm.ownerType, vm.ownerId);
    }).then(function () {
      vm.dirtySourceControl = {};
      vm.originalSourceControl = {};
      vm.sourceControlEditor.$setPristine();
      SameOwnerStateNavigationService.goEdit('edit-source-control');
      doLoad();
    });
  }

  function save() {
    vm.submitError = undefined;
    vm.scmConfigValidationResult = undefined;
    let savePromise;
    let sourceControl = modelToSourceControl(vm.dirtySourceControl);

    if (
      vm.dirtySourceControl.id &&
      vm.isApp &&
      sourceControl.repositoryUrl !== vm.originalSourceControl.repositoryUrl
    ) {
      UpdateSourceControlModalService.updateSourceControl(function () {
        return SourceControlService.updateSourceControlRecord(vm.ownerType, vm.ownerId, sourceControl);
      })
        .then(function () {
          doLoad();
        })
        .catch(function (e) {
          vm.submitError = Messages.getHttpErrorMessage(e);
        });
    } else {
      if (vm.dirtySourceControl.id) {
        savePromise = SourceControlService.updateSourceControlRecord(vm.ownerType, vm.ownerId, sourceControl);
      } else {
        savePromise = SourceControlService.addSourceControlRecord(vm.ownerType, vm.ownerId, sourceControl);
      }
      vm.sourceControlEditorMask
        .wrap(savePromise)
        .then(function () {
          doLoad();
        })
        .catch(function (e) {
          vm.submitError = Messages.getHttpErrorMessage(e);
        });
    }
  }

  function isDirty() {
    let original = modelToSourceControl(vm.originalSourceControl);
    let dirty = modelToSourceControl(vm.dirtySourceControl);
    return original !== dirty && !angular.equals(original, dirty);
  }

  function compositeSourceControlToModel(compositeSourceControl) {
    let model = {};

    model.ownerId = compositeSourceControl.ownerId;
    model.id = compositeSourceControl.id;
    model.repositoryUrl = compositeSourceControl.repositoryUrl;

    model.provider = compositeSourceControl.provider.value;
    model.providerInherit = compositeSourceControl.provider.value === null && !vm.isRootOrg;
    model.providerInheritFrom = compositeSourceControl.provider.parentName;
    model.providerInheritValue = compositeSourceControl.provider.parentValue;

    if (model.providerInheritFrom !== ROOT_ORG_NAME && compositeSourceControl.token.parentName === ROOT_ORG_NAME) {
      model.token = compositeSourceControl.token.value;
      // provider is inherited from a suborg but the token is at the root
      // so act as if token is not set
      model.tokenInherit = false;
      model.tokenInheritFrom = null;

      model.username = null;
      model.usernameInherit = false;
      model.usernameInheritFrom = null;
      model.usernameInheritedValue = null;
    } else {
      model.token = compositeSourceControl.token.value;
      model.tokenInherit = compositeSourceControl.token.value === null && !vm.isRootOrg;
      model.tokenInheritFrom = compositeSourceControl.token.parentName;

      model.username = compositeSourceControl.username.value;
      model.usernameInherit = compositeSourceControl.username.value === null && !vm.isRootOrg;
      model.usernameInheritFrom = compositeSourceControl.username.parentName;
      model.usernameInheritedValue = compositeSourceControl.username.parentValue;
    }

    model.baseBranch = getBaseBranchValue(compositeSourceControl.baseBranch.value);
    model.baseBranchInherit = compositeSourceControl.baseBranch.value === null && !vm.isRootOrg;
    model.baseBranchInheritFrom = compositeSourceControl.baseBranch.parentName;
    model.baseBranchInheritedValue = compositeSourceControl.baseBranch.parentValue;

    model.pullRequestCommentingEnabled = setDefaultIfNull(
      compositeSourceControl.pullRequestCommentingEnabled.value,
      compositeSourceControl.pullRequestCommentingEnabled.parentValue,
      true
    );
    model.pullRequestCommentingEnabledInheritFrom = compositeSourceControl.pullRequestCommentingEnabled.parentName;
    model.pullRequestCommentingEnabledInheritedValue = compositeSourceControl.pullRequestCommentingEnabled.parentValue;

    model.remediationPullRequestsEnabled = setDefaultIfNull(
      compositeSourceControl.remediationPullRequestsEnabled.value,
      compositeSourceControl.remediationPullRequestsEnabled.parentValue,
      false
    );
    model.remediationPullRequestsEnabledInheritFrom = compositeSourceControl.remediationPullRequestsEnabled.parentName;
    model.remediationPullRequestsEnabledInheritedValue =
      compositeSourceControl.remediationPullRequestsEnabled.parentValue;

    model.sourceControlEvaluationsEnabled = setDefaultIfNull(
      compositeSourceControl.sourceControlEvaluationsEnabled.value,
      compositeSourceControl.sourceControlEvaluationsEnabled.parentValue,
      true
    );
    model.sourceControlEvaluationsEnabledInheritFrom =
      compositeSourceControl.sourceControlEvaluationsEnabled.parentName;
    model.sourceControlEvaluationsEnabledInheritedValue =
      compositeSourceControl.sourceControlEvaluationsEnabled.parentValue;

    model.statusChecksEnabled = compositeSourceControl.statusChecksEnabled.value;
    model.statusChecksEnabledInheritFrom = compositeSourceControl.statusChecksEnabled.parentName;
    model.statusChecksEnabledInheritedValue = compositeSourceControl.statusChecksEnabled.parentValue;

    model.sshEnabled = compositeSourceControl.sshEnabled.value;
    model.sshEnabledInheritFrom = compositeSourceControl.sshEnabled.parentName;
    model.sshEnabledInheritedValue = compositeSourceControl.sshEnabled.parentValue;

    return model;
  }

  function setDefaultIfNull(value, parentValue, defaultValue) {
    return null === value && null == parentValue ? defaultValue : value;
  }

  function sshEnabled() {
    return vm.dirtySourceControl.sshEnabled || vm.dirtySourceControl.sshEnabledInheritedValue;
  }

  function modelToSourceControl(model) {
    let sourceControl = {};

    sourceControl.ownerId = model.ownerId;
    sourceControl.id = model.id;
    sourceControl.pullRequestCommentingEnabled = getPullRequestCommentingEnabledFlagFromModel(model);
    sourceControl.remediationPullRequestsEnabled = getRemediationPullRequestsEnabledFlagFromModel(model);
    sourceControl.sourceControlEvaluationsEnabled = getSourceControlEvaluationsEnabledFlagFromModel(model);
    sourceControl.statusChecksEnabled = true;
    sourceControl.repositoryUrl = null;
    sourceControl.sshEnabled = model.sshEnabled;

    if (vm.isApp) {
      sourceControl.repositoryUrl = model.repositoryUrl;
    }

    sourceControl.username = null;
    sourceControl.token = null;
    if (
      PROVIDERS_WITH_USERNAME.includes(model.provider) ||
      (PROVIDERS_WITH_USERNAME.includes(model.providerInheritValue) && model.providerInherit)
    ) {
      // bitbucket uses 'credentials' to gather username & password. They both move as a single block
      if ((!model.credentialsInherit || vm.isRootOrg || !model.providerInherit) && model.token && model.username) {
        sourceControl.username = model.username;
        sourceControl.token = model.token;
      }
    } else {
      // username only supported in Bitbucket & Azure DevOps
      if ((!model.tokenInherit || vm.isRootOrg || !model.providerInherit) && model.token) {
        sourceControl.token = model.token === '' ? null : model.token;
      }
    }

    sourceControl.provider = null;
    if (!model.providerInherit || (vm.isRootOrg && model.provider)) {
      sourceControl.provider = model.provider === '' ? null : model.provider;
    }

    if (!model.baseBranchInherit || (vm.isRootOrg && model.baseBranch)) {
      sourceControl.baseBranch = model.baseBranch === '' ? null : getBaseBranchValueFromModel(model);
    } else {
      sourceControl.baseBranch = null;
    }

    return sourceControl;
  }

  function effectiveProvider() {
    return vm.dirtySourceControl.providerInherit
      ? vm.originalSourceControl.providerInheritValue
      : vm.dirtySourceControl.provider;
  }

  function effectiveTokenInheritFrom() {
    return vm.dirtySourceControl.providerInherit ? vm.originalSourceControl.tokenInheritFrom : null;
  }

  function isAccessTokenRequiredOnNode() {
    return vm.isApp && !vm.effectiveTokenInheritFrom();
  }

  function providerNeedsUsername() {
    return PROVIDERS_WITH_USERNAME.includes(effectiveProvider());
  }

  function isUsernameRequiredOnNode() {
    return vm.isApp && !vm.dirtySourceControl.usernameInheritFrom && providerNeedsUsername();
  }

  function isProviderRequiredOnNode() {
    return vm.isRootOrg;
  }

  function toggleShowAdvanced() {
    vm.showAdvanced = !vm.showAdvanced || !vm.canCollapseAdvanced();
  }

  const sshUrlRegExp = /^(ssh:\/\/[^?#\s]+|[^@\s]+@[^/?#\s:]+:[^?#\s]+)$/;

  /**
   * Matches SSH URLs in one of the following formats:
   * ssh://[user@]server/path
   * user@server:path
   */
  function isSshUrl() {
    return vm.dirtySourceControl.repositoryUrl && vm.dirtySourceControl.repositoryUrl.match(sshUrlRegExp);
  }

  function canCollapseAdvanced() {
    const hasToken = vm.dirtySourceControl.tokenInherit || vm.dirtySourceControl.token;
    const hasUserName =
      vm.dirtySourceControl.usernameInherit || vm.dirtySourceControl.username || !providerNeedsUsername();
    const hasBaseBranch = vm.dirtySourceControl.baseBranchInherit || vm.dirtySourceControl.baseBranch;
    const hasProvider =
      (vm.dirtySourceControl.providerInherit || vm.dirtySourceControl.provider) && !!effectiveProvider();

    // at the app level, the 'Advanced' tab can only be collapsed when there are values for all
    // required fields. At root & suborg, these aren't required and no 'advanced' block is shown
    return vm.isApp && hasToken && hasUserName && hasBaseBranch && hasProvider;
  }

  function getBaseBranchValue(value) {
    if (!value && vm.isRootOrg) {
      return 'master';
    } else {
      return value;
    }
  }

  function getInheritText(parentName, parentValue) {
    if (parentName !== null) {
      return `Inherit from ${parentName} (${parentValue})`;
    } else {
      return 'Inherit (Not Configured)';
    }
  }

  function getEnabledDisabledInheritText(parentName, parentValue) {
    return getInheritText(parentName, parentValue ? 'Enabled' : 'Disabled');
  }

  function areSourceControlEvaluationsSupported() {
    return vm.isAutomationSupported;
  }

  function isPullRequestCommentingSupported() {
    return vm.isAutomationSupported;
  }

  function arePullRequestsSupported() {
    return (
      (!effectiveProvider() || vm.providersSupportingPullRequests.includes(effectiveProvider())) &&
      vm.isAutomationSupported
    );
  }

  function getPullRequestCommentingNotAvailableMessage() {
    return isPullRequestCommentingSupported() ? '' : 'This feature is not supported by your license';
  }

  function getPullRequestsNotAvailableMessage() {
    if (arePullRequestsSupported()) {
      return '';
    }

    return vm.isAutomationSupported
      ? 'This feature is not currently supported for ' + vm.providerTypesMap[effectiveProvider()]
      : 'This feature is not supported by your license';
  }

  function getSourceControlEvaluationsNotAvailableMessage() {
    return vm.isAutomationSupported ? '' : 'This feature is not supported by your license';
  }

  function getPullRequestCommentingEnabledFlagFromModel(model) {
    if (!vm.isRootOrg || isPullRequestCommentingSupported()) {
      return model.pullRequestCommentingEnabled;
    }

    return vm.originalSourceControl.pullRequestCommentingEnabled === null
      ? true
      : vm.originalSourceControl.pullRequestCommentingEnabled;
  }

  function getRemediationPullRequestsEnabledFlagFromModel(model) {
    if (!vm.isRootOrg || arePullRequestsSupported()) {
      return model.remediationPullRequestsEnabled;
    }

    return vm.originalSourceControl.remediationPullRequestsEnabled === null
      ? true
      : vm.originalSourceControl.remediationPullRequestsEnabled;
  }

  function getSourceControlEvaluationsEnabledFlagFromModel(model) {
    if (!vm.isRootOrg || areSourceControlEvaluationsSupported()) {
      return model.sourceControlEvaluationsEnabled;
    }

    return vm.originalSourceControl.sourceControlEvaluationsEnabled === null
      ? true
      : vm.originalSourceControl.sourceControlEvaluationsEnabled;
  }

  function getBaseBranchValueFromModel(model) {
    if (!vm.isRootOrg || arePullRequestsSupported()) {
      return model.baseBranch;
    }

    return vm.originalSourceControl.baseBranch === null ? 'master' : vm.originalSourceControl.baseBranch;
  }

  function isProviderSpecifiedAndPullRequestCommentingSupported() {
    return effectiveProvider() && isPullRequestCommentingSupported();
  }

  function isProviderSpecifiedAndPullRequestsSupported() {
    return effectiveProvider() && arePullRequestsSupported();
  }

  function isProviderSpecifiedAndSourceControlEvaluationsSupported() {
    return effectiveProvider() && areSourceControlEvaluationsSupported();
  }
}
const mapStateToThis = (state) => ({
  isAutomationSupported: selectIsSourceControlSupported(state),
  isSourceControlSupported: selectIsSourceControlForSourceTileSupported(state),
});

SourceControlEditorController.$inject = [
  'CLMContextLocations',
  'OrganizationStore',
  'ApplicationStore',
  '$q',
  'Messages',
  'SameOwnerStateNavigationService',
  'DeleteModalService',
  'SourceControlService',
  '$scope',
  'UpdateSourceControlModalService',
  '$ngRedux',
];
