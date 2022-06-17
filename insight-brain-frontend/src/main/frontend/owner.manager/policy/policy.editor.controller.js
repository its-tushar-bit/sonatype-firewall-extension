/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectCategories,
  selectCurrentPolicy,
  selectDeleteModal,
  selectHasPolicyCategories,
  selectIsCurrentPolicyDirty,
  selectIsOrgOwner,
  selectIsRootOrg,
  selectLoadError,
  selectOriginalProxyStageAction,
  selectIsInherited,
  selectSiblings,
  selectSubmitError,
  selectIsEditMode,
  selectCurrentPolicyOwnerName,
  selectLoading,
  selectIsActionOverrideEnabled,
  selectHasEditIqPermission,
  selectOverrideNeedsToBeRemoved,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { prop, propEq } from 'ramda';
import { selectPoliciesByOwner, selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { getActionsOverride } from 'MainRoot/OrgsAndPolicies/utility/util';

export default function PolicyEditorController($scope, DeleteModalService, $rootScope, EventNameConstant, $ngRedux) {
  var vm = this;

  vm.policyEditor = undefined;
  vm.policyEditorMask = undefined;
  vm.getActions = getActions;
  vm.doLoad = doLoad;
  vm.deletePolicy = deletePolicy;
  vm.save = save;
  vm.onNameChange = onNameChange;
  vm.onThreatLevelChange = onThreatLevelChange;
  vm.onCategoriesChanged = onCategoriesChanged;
  vm.onPolicyViolationGrandfatheringAllowedChange = onPolicyViolationGrandfatheringAllowedChange;
  vm.onPolicyActionsOverrideAllowedChange = onPolicyActionsOverrideAllowedChange;
  vm.onHasPolicyCategoriesChange = onHasPolicyCategoriesChange;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadPolicyEditor: actions.loadPolicyEditor,
    savePolicy: actions.savePolicy,
    saveActionsOverride: actions.saveActionsOverride,
    removeActionsOverride: actions.removeActionsOverride,
    removePolicy: actions.removePolicy,
    setPolicyName: actions.setPolicyName,
    setThreatLevel: actions.setThreatLevel,
    togglePolicyViolationGrandfatheringAllowed: actions.togglePolicyViolationGrandfatheringAllowed,
    togglePolicyActionsOverrideAllowed: actions.togglePolicyActionsOverrideAllowed,
    setHasPolicyCategories: actions.setHasPolicyCategories,
    toggleCategoryIsApplied: actions.toggleCategoryIsApplied,
    checkEditIqPermission: actions.checkEditIqPermission,
  })(vm);

  vm.doLoad();

  $scope.$on('pageChangeStarted', (event) => {
    if (vm.isPolicyDirty) {
      event.preventDefault();
    }
  });

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    vm.checkEditIqPermission();
    vm.loadPolicyEditor();
  }

  function deletePolicy() {
    const message = `You are about to permanently remove ${vm.dirtyPolicy.name}. This action cannot be undone.`;

    DeleteModalService.deleteRedux('Delete Policy', message, 'Deleting', removePolicy, selectDeleteModal);
  }

  function removePolicy() {
    vm.removePolicy().then(() => {
      $rootScope.$broadcast('resource.data.modified');
    });
  }

  function save() {
    if (vm.isActionOverrideEnabled) {
      vm.policyEditorMask.wrap(vm.overrideNeedsToBeRemoved ? vm.removeActionsOverride() : vm.saveActionsOverride());
      return;
    }

    vm.policyEditorMask.wrap(
      vm.savePolicy({
        onSaveExistingPolicy: () => {
          $rootScope.$broadcast('resource.data.modified');
          $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY, {
            resetScroll: true,
          });
          $rootScope.$broadcast('resource.data.modified');
        },
      })
    );
  }

  function onNameChange() {
    vm.setPolicyName(vm.dirtyPolicy.name);
  }

  function onThreatLevelChange() {
    vm.setThreatLevel(vm.dirtyPolicy.threatLevel);
  }

  function onCategoriesChanged(category) {
    const categoryIndexForToggle = vm.categories.findIndex(propEq('id', category.id));
    vm.toggleCategoryIsApplied(categoryIndexForToggle);
  }

  function onPolicyActionsOverrideAllowedChange() {
    vm.togglePolicyActionsOverrideAllowed();
  }

  function onPolicyViolationGrandfatheringAllowedChange() {
    vm.togglePolicyViolationGrandfatheringAllowed();
  }

  function onHasPolicyCategoriesChange(hasPolicyCategories) {
    vm.setHasPolicyCategories(hasPolicyCategories);
  }

  function getActions() {
    const ownerIds = vm.policiesByOwner.map(prop('ownerId'));
    const actionsOverrideInfo = getActionsOverride(ownerIds, vm.dirtyPolicy);

    return actionsOverrideInfo?.actionsOverride || vm.dirtyPolicy.actions;
  }
}

export const mapStateToThis = (state) => ({
  dirtyPolicy: angular.copy(selectCurrentPolicy(state)),
  categories: angular.copy(selectCategories(state)),
  isEditMode: selectIsEditMode(state),
  isPolicyDirty: selectIsCurrentPolicyDirty(state),
  loadError: selectLoadError(state),
  loading: selectLoading(state),
  submitError: selectSubmitError(state),
  siblings: selectSiblings(state),
  isOrgOwner: selectIsOrgOwner(state),
  isRootOrg: selectIsRootOrg(state),
  hasPolicyCategories: selectHasPolicyCategories(state),
  readOnly: selectIsInherited(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  originalProxyStageAction: selectOriginalProxyStageAction(state),
  ownerName: selectCurrentPolicyOwnerName(state),
  isActionOverrideEnabled: selectIsActionOverrideEnabled(state),
  currentOwnerId: selectSelectedOwnerId(state),
  hasEditIqPermission: selectHasEditIqPermission(state),
  overrideNeedsToBeRemoved: selectOverrideNeedsToBeRemoved(state),
  selectedOwnerId: selectSelectedOwnerId(state),
  policiesByOwner: selectPoliciesByOwner(state),
});

PolicyEditorController.$inject = ['$scope', 'DeleteModalService', '$rootScope', 'event.name.constant', '$ngRedux'];
