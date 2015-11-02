/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function CategoryEditorController($stateParams, $scope, TagStore, Messages, DeleteModalService, formMaskDelay, SameOwnerStateNavigationService) {
    var vm = this;
    var store;

    vm.dirtyCategory = undefined;
    vm.deleteCategory = deleteCategory;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.categoryEditor = undefined;
    vm.siblings = [];
    vm.save = save;

    vm.doLoad();

    function deleteCategory() {
      DeleteModalService.deleteResource('Category', vm.dirtyCategory.name, vm.dirtyCategory).then(function() {
        SameOwnerStateNavigationService.goEdit('create-category');
      });
    }

    function doLoad() {
      TagStore[vm.error ? 'refresh' : 'get']().then(function(tagStore) {
        tagStore.forEach(function(owner){
          vm.siblings = vm.siblings.concat(owner.tags);
        });
        store = tagStore[0].store;
        if (!$stateParams.categoryId) {
          vm.dirtyCategory = store.create();
        } else {
          tagStore[0].tags.forEach(function(categoryCandidate) {

            if (categoryCandidate.id === $stateParams.categoryId) {
              vm.dirtyCategory = categoryCandidate.$clone();
              return true;
            }
          });
        }

        if (!vm.dirtyCategory) {
          vm.error = 'Unable to locate category.';
        }
      }, function() {
        vm.error = arguments;
      });
    }

    function save() {
      var isNew = vm.dirtyCategory.$new;
      delete vm.error;

      formMaskDelay.wrap($scope, vm.dirtyCategory.$save()).then(function() {
        if (isNew) {
          vm.dirtyCategory = store.create();
        }
        vm.categoryEditor.$setPristine();
      }, function() {
        vm.error = arguments;
      });
    }
  }

  CategoryEditorController.$inject = ['$stateParams', '$scope', 'TagStore', 'Messages', 'DeleteModalService', 'FormMaskDelay', 'SameOwnerStateNavigationService'];

  angular.module('owner.manager.module').controller('category.editor.controller', CategoryEditorController);

}(angular));
