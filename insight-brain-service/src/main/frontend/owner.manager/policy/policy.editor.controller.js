/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorController($scope, $q, $http, $stateParams, PolicyHierarchyStore, DeleteModalService,
                                  SameOwnerStateNavigationService, CLMAppLocations)
  {
    var vm = this,
        originalCategories,
        originalHasPolicyCategories,
        createPolicy;

    vm.dirtyPolicy = undefined;
    vm.doLoad = doLoad;
    vm.policyEditor = undefined;
    vm.policyEditorMask = undefined;
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
    vm.readOnly = undefined;
    vm.isRootOrg = CLMAppLocations.isRootOrg();

    vm.doLoad();

    $scope.$on('pageChangeStarted', function(event) {
      if (vm.isPolicyDirty()) {
        event.preventDefault();
      }
    });

    function deletePolicy() {
      DeleteModalService.deleteResource('Policy', vm.dirtyPolicy.name, vm.dirtyPolicy).then(function() {
        // Model needs to be clean in order to navigate
        vm.dirtyPolicy.$revert();
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

          owner.policies.some(function(policyCandidate) {
            if ($stateParams.policyId === policyCandidate.id) {
              vm.readOnly = index !== 0;
              vm.dirtyPolicy = policyCandidate.$clone();
              return true;
            }
          });
        });
      }

      function loadCategories(categoriesByOwner, availableCategories) {
        vm.hasPolicyCategories = Boolean(availableCategories && availableCategories.length > 0);
        var appliedCategoriesById = vm.hasPolicyCategories ? availableCategories.map(function(category) {
          return category.id;
        }) : [];

        categoriesByOwner.forEach(function(owner) {
          vm.categories = vm.categories.concat(owner.tags);
        });
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

      if (vm.policyEditor.$valid && vm.isPolicyDirty() && !vm.readOnly) {
        var savePolicy = vm.dirtyPolicy.$save().then(function() {
          return vm.isApp ? $q.when([]) : $http.put(CLMAppLocations.getPolicyTagUrl(vm.dirtyPolicy.id),
              vm.hasPolicyCategories ? appliedCategories : []);
        }, submitErrorHandler);

        var isNew = vm.dirtyPolicy.$new;
        delete vm.submitError;

        var appliedCategories = vm.categories.filter(function(category) {
          return category.isApplied;
        });

        vm.policyEditorMask.wrap(savePolicy).then(function() {
          if (isNew) {
            vm.siblings.push(vm.dirtyPolicy);
            vm.dirtyPolicy = createPolicy();
          }

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
    '$scope', '$q', '$http', '$stateParams', 'PolicyHierarchyStore', 'DeleteModalService',
    'SameOwnerStateNavigationService', 'CLMAppLocations'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.controller', PolicyEditorController);

}(angular));
