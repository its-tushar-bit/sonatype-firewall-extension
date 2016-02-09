/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function CategoryEditorController($stateParams, TagStore, DeleteModalService, SameOwnerStateNavigationService) {
    var vm = this;
    var store;

    vm.dirtyCategory = undefined;
    vm.deleteCategory = deleteCategory;
    vm.doLoad = doLoad;
    vm.loadError = undefined;
    vm.categoryEditor = undefined;
    vm.categoryEditorMask = undefined;
    vm.siblings = [];
    vm.save = save;
    vm.submitError = undefined;

    vm.doLoad();

    function deleteCategory() {
      DeleteModalService.deleteResource('Category', vm.dirtyCategory.name, vm.dirtyCategory).then(function() {
        SameOwnerStateNavigationService.goEdit('create-category');
      });
    }

    function doLoad() {
      TagStore[vm.loadError ? 'refresh' : 'get']().then(function(tagStore) {
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
          vm.loadError = 'Unable to locate category.';
        }
      }, function(error) {
        vm.loadError = error;
      });
      delete vm.loadError;
    }

    function save() {
      var isNew = vm.dirtyCategory.$new;
      delete vm.submitError;

      vm.categoryEditorMask.wrap(vm.dirtyCategory.$save()).then(function() {
        if (isNew) {
          vm.siblings.push(vm.dirtyCategory);
          vm.dirtyCategory = store.create();
        }
        vm.categoryEditor.$setPristine();
      }, function(error) {
        vm.submitError = error;
      });
    }
  }

  CategoryEditorController.$inject = [
    '$stateParams', 'TagStore', 'DeleteModalService', 'SameOwnerStateNavigationService'
  ];

  angular.module('owner.manager.module').controller('category.editor.controller', CategoryEditorController);

}(angular));
