/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  selectLicenseThreatGroupSlice,
  selectIsLoading,
  selectLicenseThreatGroupIsEditMode,
  selectLicenseThreatGroupIsDirty,
  selectLicenseThreatGroupSubmitError,
  selectLicenseThreatGroupLoadError,
  selectLicenseThreatGroupSiblings,
  selectNextLicenseThreatGroup,
  selectDirtyLicenseThreatGroup,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';

export default function LicenseThreatGroupEditorController(_$rootScope, $scope, $state, DeleteModalService, $ngRedux) {
  const vm = this;
  vm.ltgEditor = undefined;
  vm.ltgEditorMask = undefined;
  vm.doLoad = doLoad;
  vm.save = save;
  vm.deleteLTG = deleteLTG;
  vm.getTooltip = getTooltip;
  vm.onNameChange = onNameChange;
  vm.onThreatLevelChange = onThreatLevelChange;
  vm.onPickedLicensesChange = onPickedLicensesChange;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    setLicenseThreatGroupName: actions.setLicenseThreatGroupName,
    setLicenseThreatGroupThreatLevel: actions.setLicenseThreatGroupThreatLevel,
    setPickedLicenses: actions.setPickedLicenses,
    loadLicenseThreatGroupEditor: actions.loadLicenseThreatGroupEditor,
    removeLicenseThreatGroup: actions.removeLicenseThreatGroup,
    resetDeleteModalState: actions.resetDeleteModalState,
    saveLicenseThreatGroup: actions.saveLicenseThreatGroup,
  })(vm);

  $scope.$on('pageChangeStarted', (event) => {
    if (vm.isDirty) {
      event.preventDefault();
    }
  });
  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  vm.doLoad();

  function getTooltip(item) {
    return item.longDisplayName;
  }

  function onNameChange() {
    vm.setLicenseThreatGroupName(vm.dirtyLTG.name);
  }

  function onThreatLevelChange() {
    vm.setLicenseThreatGroupThreatLevel(vm.dirtyLTG.threatLevel);
  }

  function onPickedLicensesChange() {
    vm.setPickedLicenses(vm.dirtyLTG.pickedLicenses);
  }

  function doLoad() {
    vm.loadLicenseThreatGroupEditor($state);
  }

  function deleteLTG() {
    const message = `You are about to permanently remove ${vm.dirtyLTG.name}. This action cannot be undone.`;
    vm.resetDeleteModalState();

    DeleteModalService.deleteRedux(
      'Delete License Threat Group',
      message,
      'Deleting',
      () => vm.removeLicenseThreatGroup($state),
      selectLicenseThreatGroupSlice
    );
  }

  function save() {
    vm.ltgEditorMask.wrap(
      vm.saveLicenseThreatGroup({
        setPristine: () => {
          vm.ltgEditor.$setPristine();
        },
        $state,
      })
    );
  }
}

// angular.copy usage is temp solution in order to edit immutable data from redux with angular forms
// must be removed once the component is migrated to React
export const mapStateToThis = (state) => {
  return {
    nextLTG: angular.copy(selectNextLicenseThreatGroup(state)),
    dirtyLTG: angular.copy(selectDirtyLicenseThreatGroup(state)),
    loadError: selectLicenseThreatGroupLoadError(state),
    submitError: selectLicenseThreatGroupSubmitError(state),
    loading: selectIsLoading(state),
    isEditMode: selectLicenseThreatGroupIsEditMode(state),
    siblings: selectLicenseThreatGroupSiblings(state),
    isDirty: selectLicenseThreatGroupIsDirty(state),
  };
};

LicenseThreatGroupEditorController.$inject = ['$rootScope', '$scope', '$state', 'DeleteModalService', '$ngRedux'];
