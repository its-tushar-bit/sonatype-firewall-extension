/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions as ownerEditorActions } from 'MainRoot/OrgsAndPolicies/ownerEditorSlice';

export default function ChangeApplicationIdController(
  $scope,
  $rootScope,
  $state,
  owner,
  siblings,
  Messages,
  OwnerConstant,
  EventNameConstant,
  $ngRedux
) {
  var vm = this;

  vm.isDirty = isDirty;
  vm.changeApplicationId = changeApplicationId;
  vm.originalApp = angular.copy(owner);
  vm.dirtyApp = {
    publicId: null,
  };
  vm.error = undefined;
  vm.siblings = siblings;
  vm.applicationIdEditorMask = undefined;
  vm.unsavedModalVisible = false;
  vm.unsubscribe = $ngRedux.connect(null, { updateOwner: ownerEditorActions.updateOwner })(vm);

  // Override messages to be used in the field validation popover
  const invalidCharactersMessage = 'Use valid characters: alphanumeric, "_", "." or "-"';
  vm.formMessages = {
    duplicate: 'ID is already in use',
    validNameCharacters: invalidCharactersMessage,
    noSpaces: invalidCharactersMessage,
  };

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.isDirty()) {
      vm.unsavedModalVisible = true;
      event.preventDefault();
    }
  });

  $scope.$on('pageChangeCanceled', function () {
    vm.unsavedModalVisible = false;
  });

  $scope.$on('pageChangeAccepted', function () {
    $scope.$dismiss();
  });

  function changeApplicationId() {
    if (!isDirty()) {
      return;
    }
    delete vm.error;
    vm.originalApp.publicId = vm.dirtyApp.publicId;

    vm.applicationIdEditorMask
      .wrap(vm.updateOwner({ ownerToSave: vm.originalApp, isApp: true }).then(unwrapResult))
      .then(
        function () {
          $scope.$close();
          $rootScope.$broadcast(
            EventNameConstant.RELOAD_OWNER_TREE_DATA,
            vm.originalApp,
            OwnerConstant.APPLICATION_TYPE,
            false
          );
          $state.go('management.view.application', {
            applicationPublicId: vm.originalApp.publicId,
          });
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
  }

  function isDirty() {
    return vm.dirtyApp.publicId !== null && vm.dirtyApp.publicId !== vm.originalApp.publicId;
  }
}

ChangeApplicationIdController.$inject = [
  '$scope',
  '$rootScope',
  '$state',
  'owner',
  'siblings',
  'Messages',
  'owner.constant',
  'event.name.constant',
  '$ngRedux',
];
