/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSlice';
import {
  selectLabelsLoading,
  selectLabelsLoadError,
  selectLabelsIsEditMode,
  selectLabelsSiblings,
  selectLabelsIsDirty,
  selectLabelsCurrentLabel,
  selectLabelsSubmitError,
  selectLabelsSlice,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSelectors';

export default function LabelEditorController($rootScope, $scope, DeleteModalService, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    labelEditor: undefined,
    labelEditorMask: undefined,

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
        loadLabelsEditor: actions.loadLabelsEditor,
        setLabelDescription: actions.setLabelDescription,
        setLabelName: actions.setLabelName,
        setLabelColor: actions.setLabelColor,
        saveLabel: actions.saveLabel,
        removeLabel: actions.removeLabel,
        resetDeleteModalState: actions.resetDeleteModalState,
      })(vm);

      $scope.$on('pageChangeStarted', (event) => {
        if (vm.isDirty) {
          event.preventDefault();
        }
      });

      vm.doLoad();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    doLoad() {
      vm.loadLabelsEditor();
    },

    deleteLabel() {
      const message = `You are about to permanently remove ${vm.dirtyLabel.label}. This action cannot be undone.`;
      vm.resetDeleteModalState();

      DeleteModalService.deleteRedux(
        'Delete Label',
        message,
        'Deleting',
        () => {
          vm.removeLabel(() => {
            $rootScope.$broadcast('label.saved');
          });
        },
        selectLabelsSlice
      );
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
      vm.labelEditorMask
        .wrap(
          vm.saveLabel({
            setPristine: () => {
              vm.labelEditor.$setPristine();
            },
          })
        )
        .then(({ payload }) => {
          if (payload.label) {
            // TODO: should be removed once OwnerDetailTreeViewController is updated to use Redux
            $rootScope.$broadcast('label.saved');
          }
        });
    },
  });
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

LabelEditorController.$inject = ['$rootScope', '$scope', 'DeleteModalService', '$ngRedux'];
