/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import {
  selectLabelsLoading,
  selectLabelsLoadError,
  selectLabelsIsEditMode,
  selectLabelsSiblings,
  selectLabelsIsDirty,
  selectLabelsCurrentLabel,
  selectLabelsSubmitError,
  selectLabelsSlice,
} from 'MainRoot/OrgsAndPolicies/labelsSelectors';

export default function LabelEditorController($scope, DeleteModalService, $ngRedux) {
  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadLabelsEditor: actions.loadLabelsEditor,
    setLabelDescription: actions.setLabelDescription,
    setLabelName: actions.setLabelName,
    setLabelColor: actions.setLabelColor,
    saveLabel: actions.saveLabel,
    removeLabel: actions.removeLabel,
    resetDeleteModalState: actions.resetDeleteModalState,
  })(vm);

  Object.assign(vm, {
    labelEditor: undefined,
    labelEditorMask: undefined,

    doLoad() {
      vm.loadLabelsEditor();
    },

    deleteLabel() {
      const message = `You are about to permanently remove ${vm.dirtyLabel.label}. This action cannot be undone.`;
      vm.resetDeleteModalState();

      DeleteModalService.deleteRedux('Delete Label', message, 'Deleting', vm.removeLabel, selectLabelsSlice);
    },

    onDescriptionChange() {
      vm.setLabelDescription(vm.dirtyLabel.description);
    },

    onNameChange() {
      vm.setLabelName(vm.dirtyLabel.label);
    },

    onColorChange() {
      vm.setLabelColor(vm.dirtyLabel.color);
    },

    save() {
      vm.labelEditorMask.wrap(
        vm.saveLabel({
          setPristine: () => {
            vm.labelEditor.$setPristine();
          },
        })
      );
    },
  });

  $scope.$on('pageChangeStarted', (event) => {
    if (vm.isDirty) {
      event.preventDefault();
    }
  });

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  vm.doLoad();
}

// angular.copy usage is temp solution in order to edit immutable data from redux with angular forms
// must be removed once the component is migrated to React
export const mapStateToThis = (state) => {
  return {
    dirtyLabel: angular.copy(selectLabelsCurrentLabel(state)),
    loadError: selectLabelsLoadError(state),
    submitError: selectLabelsSubmitError(state),
    loading: selectLabelsLoading(state),
    isEditMode: selectLabelsIsEditMode(state),
    siblings: selectLabelsSiblings(state),
    isDirty: selectLabelsIsDirty(state),
  };
};

LabelEditorController.$inject = ['$scope', 'DeleteModalService', '$ngRedux'];
