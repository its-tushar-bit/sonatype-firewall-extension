/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';

export default function PolicyEditorController(
  $scope,
  $q,
  $http,
  $stateParams,
  PolicyHierarchyStore,
  TagStore,
  DeleteModalService,
  SameOwnerStateNavigationService,
  CLMContextLocations,
  $rootScope,
  EventNameConstant,
  $state,
  ProductFeatures
) {
  var vm = this,
    originalCategories,
    originalHasPolicyCategories,
    policyStores,
    isReloading = false;

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
  vm.isOrgOwner = undefined;
  vm.hasPolicyCategories = false;
  vm.submitError = undefined;
  vm.owner = undefined;
  vm.readOnly = undefined;
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.isGrandfatheringSupported = undefined;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (!isReloading && vm.isPolicyDirty()) {
      event.preventDefault();
    }
  });

  function deletePolicy() {
    DeleteModalService.deleteResource(
      'Policy',
      vm.dirtyPolicy.name,
      vm.dirtyPolicy
    ).then(function () {
      // Model needs to be clean in order to navigate
      vm.dirtyPolicy.$revert();
      SameOwnerStateNavigationService.goEdit('create-policy');
    });
  }

  function doLoad() {
    var promises = [PolicyHierarchyStore.get(), ProductFeatures.load()];

    if ($stateParams.policyId) {
      promises.push(PolicyHierarchyStore.getById($stateParams.policyId));
    }

    $q.all(promises).then(
      function (results) {
        policyStores = results[0];

        policyStores.forEach(function (owner) {
          vm.siblings = vm.siblings.concat(owner.policies);
        });

        vm.isGrandfatheringSupported = ProductFeatures.isAvailable(
          'policy-grandfathering'
        );
        if (results.length > 2) {
          vm.dirtyPolicy = results[2].$clone();

          policyStores.some(function (owner, index) {
            if (owner.policies.indexOf(results[2]) !== -1) {
              vm.readOnly = index !== 0;

              vm.owner = {
                id: owner.ownerId,
                name: owner.ownerName,
              };

              vm.isOrgOwner = owner.ownerType === 'organization';
              return true;
            }
          });

          vm.isRootOrg = vm.owner.id === 'ROOT_ORGANIZATION_ID';
        } else {
          vm.dirtyPolicy = policyStores[0].store.create();
          vm.owner = {
            id: policyStores[0].ownerId,
            name: policyStores[0].ownerName,
          };
          vm.isOrgOwner = CLMContextLocations.isOrganization();
        }

        vm.categories = [];

        if (vm.isOrgOwner) {
          var promises = [TagStore.get()];

          // A newly created policy won't have any tags associated with it
          if ($stateParams.policyId) {
            promises.push(
              $http.get(
                CLMContextLocations.getPolicyTagUrl($stateParams.policyId)
              )
            );
          }

          $q.all(promises).then(
            function (results) {
              loadCategories(
                results[0],
                results.length > 1 ? results[1].data : undefined
              );
            },
            function (error) {
              vm.loadError = error;
            }
          );
        }
      },
      function (error) {
        vm.loadError = error;
      }
    );

    delete vm.loadError;

    function loadCategories(categoriesByOwner, availableCategories) {
      vm.hasPolicyCategories = Boolean(
        availableCategories && availableCategories.length > 0
      );

      var appliedCategoriesById = vm.hasPolicyCategories
        ? availableCategories.map(function (category) {
            return category.id;
          })
        : [];

      var startConcat = false;
      categoriesByOwner.forEach(function (owner) {
        //we only want to append categories that are actually part of the owner of the policy being shown.  We don't
        //want to show tags from children when showing a parent policy in read only mode
        if (
          vm.dirtyPolicy &&
          (!vm.dirtyPolicy.ownerId || vm.dirtyPolicy.ownerId === owner.ownerId)
        ) {
          startConcat = true;
        }
        if (startConcat) {
          vm.categories = vm.categories.concat(owner.applicationCategories);
        }
      });
      vm.categories.forEach(function (category) {
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
      var savePolicy = vm.dirtyPolicy.$save().then(function () {
        return !vm.isOrgOwner
          ? $q.when([])
          : $http.put(
              CLMContextLocations.getPolicyTagUrl(vm.dirtyPolicy.id),
              vm.hasPolicyCategories ? appliedCategories : []
            );
      }, submitErrorHandler);

      var isNew = vm.dirtyPolicy.$new;
      delete vm.submitError;

      var appliedCategories = vm.categories
        .filter(prop('isApplied'))
        .map((c) => c.$getOriginal());

      vm.policyEditorMask.wrap(savePolicy).then(function () {
        if (isNew) {
          isReloading = true;
          $state.reload();
        } else {
          originalCategories = angular.copy(vm.categories);
          originalHasPolicyCategories = vm.hasPolicyCategories;
          vm.policyEditor.$setPristine();
          $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY, {
            resetScroll: true,
          });
        }
      }, submitErrorHandler);
    }
  }

  function isInheritanceSectionDirty() {
    return (
      vm.isOrgOwner &&
      ((vm.hasPolicyCategories &&
        !angular.equals(originalCategories, vm.categories)) ||
        originalHasPolicyCategories !== vm.hasPolicyCategories)
    );
  }

  function isPolicyDirty() {
    return vm.dirtyPolicy.isDirty() || isInheritanceSectionDirty();
  }
}

PolicyEditorController.$inject = [
  '$scope',
  '$q',
  '$http',
  '$stateParams',
  'PolicyHierarchyStore',
  'TagStore',
  'DeleteModalService',
  'SameOwnerStateNavigationService',
  'CLMContextLocations',
  '$rootScope',
  'event.name.constant',
  '$state',
  'ProductFeatures',
];
