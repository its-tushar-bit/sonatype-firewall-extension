/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function CategoryEditorController($q, $stateParams, TagStore, ApplicationStore, DeleteModalService, SameOwnerStateNavigationService) {
    var vm = this,
        store,
        associatedAppNames = [],
        warningMessage;

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
      DeleteModalService.deleteCustom('Delete Category', warningMessage, 'Deleting',
          angular.bind(vm.dirtyCategory, vm.dirtyCategory.$delete)).then(function() {
        SameOwnerStateNavigationService.goEdit('create-category');
      });
    }

    function doLoad() {
      var promises = [TagStore[vm.loadError ? 'refresh' : 'get'](), TagStore.getApplied(), ApplicationStore.get()];
      $q.all(promises).then(function(results) {
        var tagStore = results[0];

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
              // gather the names of associated applications
              results[1].data.applicationTagsByOwner[0].applicationTags.forEach(function(applicationTag) {
                results[2].forEach(function(application) {
                  if (application.id === applicationTag.applicationId) {
                    associatedAppNames.push(application.name);
                  }
                });
              });
              warningMessage = 'Are you sure you want to delete this application category?';
              if (associatedAppNames.length > 0) {
                warningMessage += ' It is in use by the following applications: ' + associatedAppNames.join(', ') + '.';
              }
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
    '$q', '$stateParams', 'TagStore', 'ApplicationStore', 'DeleteModalService', 'SameOwnerStateNavigationService'
  ];

  angular.module('owner.manager.module').controller('category.editor.controller', CategoryEditorController);

}(angular));
