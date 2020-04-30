/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import deleteTemplate from './delete.application.category.error.modal.html';

export default
function CategoryEditorController($scope, $stateParams, Modal, TagStore, DeleteModalService,
                                  SameOwnerStateNavigationService, $q, PolicyTagStore, PolicyHierarchyStore,
                                  ApplicationStore) {
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
  vm.tagPolicyList = [];

  vm.doLoad();

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.dirtyCategory.isDirty()) {
      event.preventDefault();
    }
  });

  function deleteCategory() {
    if (vm.tagPolicyList.length) {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        template: deleteTemplate,
        scope: $scope
      });
    }
    else {
      DeleteModalService.deleteCustom('Delete Application Category', warningMessage, 'Deleting',
          angular.bind(vm.dirtyCategory, vm.dirtyCategory.$delete)).then(function() {
        // Model needs to be clean in order to navigate
        vm.dirtyCategory.$revert();
        SameOwnerStateNavigationService.goEdit('create-category');
      });
    }
  }

  function doLoad() {
    var promises = [
          TagStore[vm.loadError ? 'refresh' : 'get'](), TagStore.getApplied(), ApplicationStore.get(),
          PolicyHierarchyStore.get(), PolicyTagStore.getApplied()
        ],
        policyMap = {};

    if ($stateParams.categoryId) {
      promises.push(TagStore.getById($stateParams.categoryId));
    }

    $q.all(promises).then(function(results) {
      results[0].forEach(function(owner) {
        vm.siblings = vm.siblings.concat(owner.applicationCategories);
      });

      //the first owner is the local one
      var owner = results[0][0];
      store = owner.store;
      if (!$stateParams.categoryId) {
        vm.dirtyCategory = store.create();
      }
      else {
        vm.dirtyCategory = results[5].$clone();

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
        vm.tagPolicyList = [];

        results[4].data.forEach(function(policyTag) {
          if (policyTag.tagId === $stateParams.categoryId) {
            vm.tagPolicyList.push(policyMap[policyTag.policyId]);
          }
        });
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
  '$scope', '$stateParams', 'Modal', 'TagStore', 'DeleteModalService', 'SameOwnerStateNavigationService', '$q',
  'PolicyTagStore', 'PolicyHierarchyStore', 'ApplicationStore'
];
