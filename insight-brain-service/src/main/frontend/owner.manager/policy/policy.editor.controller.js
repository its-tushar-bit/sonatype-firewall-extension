/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorController($q, $http, $scope, $stateParams, PolicyHierarchyStore, DeleteModalService,
                                  formMaskDelay, SameOwnerStateNavigationService, CLMAppLocations)
  {
    var vm = this,
        originalCategories,
        originalHasPolicyCategories,
        createPolicy;

    vm.dirtyPolicy = undefined;
    vm.doLoad = doLoad;
    vm.policyEditor = undefined;
    vm.deletePolicy = deletePolicy;
    vm.loadError = undefined;
    vm.save = save;
    vm.isPolicyDirty = isPolicyDirty;
    vm.siblings = [];
    vm.categories = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.hasPolicyCategories = false;
    vm.submitError = undefined;
    vm.ownerName = undefined;

    vm.doLoad();

    function deletePolicy() {
      DeleteModalService.deleteResource('Policy', vm.dirtyPolicy.name, vm.dirtyPolicy).then(function() {
        SameOwnerStateNavigationService.goEdit('create-policy');
      });
    }

    function doLoad() {
      var promises = [
        PolicyHierarchyStore.get()
      ];

      if (!vm.isApp) {
        promises.push($http.get(CLMAppLocations.getTagsUrl()));
        if ($stateParams.policyId) {
          promises.push($http.get(CLMAppLocations.getPolicyTagUrl($stateParams.policyId)));
        }
      }

      $q.all(promises).then(function(results) {
        loadPolicy(results[0]);

        vm.categories = [];
        if (!vm.isApp) {
          loadCategories(results[1].data.tagsByOwner, results[2] && results[2].data);
        }

        if (!vm.dirtyPolicy) {
          vm.loadError = 'Unable to locate Policy.';
        }
      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;

      function loadPolicy(policyHierarchy) {
        createPolicy = policyHierarchy[0].store.create;
        vm.ownerName = policyHierarchy[0].ownerName;

        if (!$stateParams.policyId) {
          vm.dirtyPolicy = createPolicy();
        }

        policyHierarchy.forEach(function(owner, index) {
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
      }

      function loadCategories(categoriesByOwner, availableCategories) {
        vm.hasPolicyCategories = Boolean(availableCategories && availableCategories.length > 0);
        var appliedCategoriesById = vm.hasPolicyCategories ? availableCategories.map(function(category) {
          return category.id;
        }) : [];
        vm.categories = categoriesByOwner[0].tags;
        vm.categories.forEach(function(category) {
          category.isApplied = appliedCategoriesById.indexOf(category.id) > -1;
        });

        originalHasPolicyCategories = vm.hasPolicyCategories;
        originalCategories = angular.copy(vm.categories);
      }
    }

    function save() {
      function submitErrorHandler(error) {
        vm.submitError = error;
        return $q.reject(error);
      }

      if (vm.policyEditor.$valid && vm.isPolicyDirty()) {
        var savePolicy = vm.dirtyPolicy.$save().then(function() {
          if (isNew) {
            vm.siblings.push(vm.dirtyPolicy);
            vm.dirtyPolicy = createPolicy();
          }

          return vm.isApp ? $q.when([]) : $http.put(CLMAppLocations.getPolicyTagUrl(vm.dirtyPolicy.id),
              vm.hasPolicyCategories ? appliedCategories : []);
        }, submitErrorHandler);

        var isNew = vm.dirtyPolicy.$new;
        delete vm.submitError;

        var appliedCategories = vm.categories.filter(function(category) {
          return category.isApplied;
        });

        formMaskDelay.wrap($scope, savePolicy).then(function() {
          originalCategories = angular.copy(vm.categories);
          originalHasPolicyCategories = vm.hasPolicyCategories;
          vm.policyEditor.$setPristine();
        }, submitErrorHandler);
      }
    }

    function isInheritanceSectionDirty() {
      return !vm.isApp && ((vm.hasPolicyCategories && !angular.equals(originalCategories, vm.categories)) ||
          (originalHasPolicyCategories !== vm.hasPolicyCategories));
    }

    function isPolicyDirty() {
      return vm.dirtyPolicy.isDirty() || isInheritanceSectionDirty();
    }
  }

  PolicyEditorController.$inject = [
    '$q', '$http', '$scope', '$stateParams', 'PolicyHierarchyStore', 'DeleteModalService', 'FormMaskDelay',
    'SameOwnerStateNavigationService', 'CLMAppLocations'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.controller', PolicyEditorController);

}(angular));
