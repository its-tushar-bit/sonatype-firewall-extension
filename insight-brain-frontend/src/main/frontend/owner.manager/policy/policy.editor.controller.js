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
  selectReadOnly,
  selectSiblings,
  selectSubmitError,
  selectIsEditMode,
  selectLoading,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { propEq } from 'ramda';

export default function PolicyEditorController($scope, DeleteModalService, $rootScope, EventNameConstant, $ngRedux) {
  var vm = this;

  vm.policyEditor = undefined;
  vm.policyEditorMask = undefined;
  vm.doLoad = doLoad;
  vm.deletePolicy = deletePolicy;
  vm.save = save;
  vm.onNameChange = onNameChange;
  vm.onThreatLevelChange = onThreatLevelChange;
  vm.onCategoriesChanged = onCategoriesChanged;
  vm.onPolicyViolationGrandfatheringAllowedChange = onPolicyViolationGrandfatheringAllowedChange;
  vm.onHasPolicyCategoriesChange = onHasPolicyCategoriesChange;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadPolicyEditor: actions.loadPolicyEditor,
    savePolicy: actions.savePolicy,
    removePolicy: actions.removePolicy,
    setPolicyName: actions.setPolicyName,
    setThreatLevel: actions.setThreatLevel,
    togglePolicyViolationGrandfatheringAllowed: actions.togglePolicyViolationGrandfatheringAllowed,
    setHasPolicyCategories: actions.setHasPolicyCategories,
    toggleCategoryIsApplied: actions.toggleCategoryIsApplied,
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
    vm.loadPolicyEditor();
  }

  function deletePolicy() {
    const message = `You are about to permanently remove ${vm.dirtyPolicy.name}. This action cannot be undone.`;

    DeleteModalService.deleteRedux('Delete Policy', message, 'Deleting', vm.removePolicy, selectDeleteModal);
  }

  function save() {
    vm.policyEditorMask.wrap(
      vm.savePolicy({
        onSaveExistingPolicy: () => {
          $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY, {
            resetScroll: true,
          });
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

  function onPolicyViolationGrandfatheringAllowedChange() {
    vm.togglePolicyViolationGrandfatheringAllowed();
  }

  function onHasPolicyCategoriesChange(hasPolicyCategories) {
    vm.setHasPolicyCategories(hasPolicyCategories);
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
  readOnly: selectReadOnly(state),
  isGrandfatheringSupported: selectIsGrandfatheringSupported(state),
  originalProxyStageAction: selectOriginalProxyStageAction(state),
  ownerName: selectOwnerName(state),
});

PolicyEditorController.$inject = ['$scope', 'DeleteModalService', '$rootScope', 'event.name.constant', '$ngRedux'];
