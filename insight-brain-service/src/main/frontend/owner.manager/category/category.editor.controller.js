/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function CategoryEditorController($stateParams, TagStore, DeleteModalService, SameOwnerStateNavigationService, $q,
                                    PolicyTagStore, PolicyHierarchyStore, ApplicationStore, ErrorModalService)
  {
    var vm = this,
        store,
        tagPolicyList = [],
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
      if (tagPolicyList.length) {
        ErrorModalService.show('Delete Application Category',
            'You cannot delete this application category because it is associated with the following policies: ' +
            tagPolicyList.join(', '));
      }
      else {
        DeleteModalService.deleteCustom('Delete Category', warningMessage, 'Deleting',
          angular.bind(vm.dirtyCategory, vm.dirtyCategory.$delete)).then(function() {
          SameOwnerStateNavigationService.goEdit('create-category');
        });
      }
    }

    function doLoad() {
      var promises = [
        TagStore[vm.loadError ? 'refresh' : 'get'](), TagStore.getApplied(), ApplicationStore.get(),
        PolicyHierarchyStore.get(), PolicyTagStore.getApplied()
      ], policyMap = {};

      $q.all(promises).then(function(results) {
        results[0].forEach(function(owner) {
          vm.siblings = vm.siblings.concat(owner.tags);
        });

        //the first owner is the local one
        var owner = results[0][0];
        store = owner.store;
        if (!$stateParams.categoryId) {
          vm.dirtyCategory = store.create();
        }
        else {
          owner.tags.some(function(tag) {
            if (tag.id === $stateParams.categoryId) {
              vm.dirtyCategory = tag.$clone();
              // gather the names of associated applications
              results[1].data.applicationTagsByOwner[0].applicationTags.forEach(function(applicationTag) {
                if (applicationTag.tagId === vm.dirtyCategory.id) {
                  results[2].forEach(function(application) {
                    if (application.id === applicationTag.applicationId) {
                      associatedAppNames.push(application.name);
                    }
                  });
                }
              });
              warningMessage = 'Are you sure you want to delete this application category?';
              if (associatedAppNames.length > 0) {
                warningMessage += ' It is in use by the following applications: ' + associatedAppNames.join(', ') + '.';
              }
              //gather a map of policy id/names
              results[3].forEach(function(owner) {
                owner.policies.forEach(function(policy) {
                  policyMap[policy.id] = policy.name;
                });
              });
              //gather list of policy names using this application category
              results[4].data.forEach(function(policyTag) {
                if (policyTag.tagId === $stateParams.categoryId) {
                  tagPolicyList.push(policyMap[policyTag.policyId]);
                }
              });
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
    '$stateParams', 'TagStore', 'DeleteModalService', 'SameOwnerStateNavigationService', '$q', 'PolicyTagStore',
    'PolicyHierarchyStore', 'ApplicationStore', 'ErrorModalService'
  ];

  angular.module('owner.manager.module').controller('category.editor.controller', CategoryEditorController);

}(angular));
