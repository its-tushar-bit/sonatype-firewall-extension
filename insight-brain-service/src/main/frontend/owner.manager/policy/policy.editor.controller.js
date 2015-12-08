/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorController($scope, $stateParams, PolicyHierarchyStore, DeleteModalService, formMaskDelay,
                                  SameOwnerStateNavigationService)
  {
    var vm = this,
        createPolicy;

    vm.dirtyPolicy = undefined;
    vm.doLoad = doLoad;
    vm.policyEditor = undefined;
    vm.deletePolicy = deletePolicy;
    vm.loadError = undefined;
    vm.save = save;
    vm.siblings = [];
    vm.submitError = undefined;

    vm.doLoad();

    function deletePolicy() {
      DeleteModalService.deleteResource('Policy', vm.dirtyPolicy.name, vm.dirtyPolicy).then(function() {
        SameOwnerStateNavigationService.goEdit('create-policy');
      });
    }

    function doLoad() {
      PolicyHierarchyStore.get().then(function(store) {
        createPolicy = store[0].store.create;

        if (!$stateParams.policyId) {
          vm.dirtyPolicy = createPolicy();
        }

        store.forEach(function(owner, index) {
          vm.siblings = vm.siblings.concat(owner.policies);

          if ($stateParams.policyId && index === 0) {
            owner.policies.some(function(policyCandidate) {
              if ($stateParams.policyId === policyCandidate.id) {
                vm.dirtyPolicy = policyCandidate.$clone();
                return true;
              }
            });
          }
        });

        if (!vm.dirtyPolicy) {
          vm.loadError = 'Unable to locate Policy.';
        }
      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
    }

    function save() {
      if (vm.policyEditor.$valid && vm.dirtyPolicy.isDirty()) {
        var isNew = vm.dirtyPolicy.$new;
        delete vm.submitError;

        formMaskDelay.wrap($scope, vm.dirtyPolicy.$save()).then(function() {
          if (isNew) {
            vm.siblings.push(vm.dirtyPolicy);
            vm.dirtyPolicy = createPolicy();
          }

          vm.policyEditor.$setPristine();
        }, function(error) {
          vm.submitError = error;
        });
      }
    }
  }

  PolicyEditorController.$inject = [
    '$scope', '$stateParams', 'PolicyHierarchyStore', 'DeleteModalService', 'FormMaskDelay',
    'SameOwnerStateNavigationService'
  ];

  angular
      .module('owner.manager.module')
      .controller('policy.editor.controller', PolicyEditorController);

}(angular));
