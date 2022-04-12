/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import deleteTemplate from './delete.application.category.error.modal.html';
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesApplicationCategoriesSlice';
import {
  selectIsDirty,
  selectIsEditMode,
  selectLoadError,
  selectIsLoading,
  selectSiblings,
  selectTagPolicyList,
  selectAssociatedApplicationNames,
  selectCurrentCategory,
  selectDeleteModal,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesApplicationCategoriesSelectors';

export default function CategoryEditorController($scope, Modal, DeleteModalService, ApplicationStore, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadCategoryEditor: actions.loadCategoryEditor,
    saveApplicationCategory: actions.saveApplicationCategory,
    removeApplicationCategory: actions.removeApplicationCategory,
    setCategoryDescription: actions.setCategoryDescription,
    setCategoryName: actions.setCategoryName,
    setCategoryColor: actions.setCategoryColor,
  })(vm);

  Object.assign(vm, {
    categoryEditor: undefined,
    categoryEditorMask: undefined,

    deleteCategory() {
      const showCannotDeleteModal = vm.tagPolicyList.length;
      if (showCannotDeleteModal) {
        Modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          template: deleteTemplate,
          scope: $scope,
        });
      } else {
        let warningMessage = 'Are you sure you want to delete this application category? ';
        if (vm.associatedApplicationNames.length) {
          warningMessage += `It is in use by the following applications: ${vm.associatedApplicationNames.join(', ')}.`;
        }
        DeleteModalService.deleteRedux(
          'Delete Application Category',
          warningMessage,
          'Deleting',
          vm.removeApplicationCategory,
          selectDeleteModal
        );
      }
    },

    doLoad() {
      const categoryEditorPromises = [ApplicationStore.get()];

      vm.loadCategoryEditor({ categoryEditorPromises });
    },
    onDescriptionChange() {
      vm.setCategoryDescription(vm.dirtyCategory.description);
    },

    onNameChange() {
      vm.setCategoryName(vm.dirtyCategory.name);
    },

    onColorChange() {
      vm.setCategoryColor(vm.dirtyCategory.color);
    },
    save() {
      vm.categoryEditorMask.wrap(
        vm.saveApplicationCategory({
          resetCategoryEditor: () => {
            vm.categoryEditor.$setPristine();
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

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  dirtyCategory: angular.copy(selectCurrentCategory(state)),
  isDirty: selectIsDirty(state),
  loadError: selectLoadError(state),
  loading: selectIsLoading(state),
  isEditMode: selectIsEditMode(state),
  siblings: selectSiblings(state),
  tagPolicyList: selectTagPolicyList(state),
  associatedApplicationNames: selectAssociatedApplicationNames(state),
});

CategoryEditorController.$inject = ['$scope', 'Modal', 'DeleteModalService', 'ApplicationStore', '$ngRedux'];
