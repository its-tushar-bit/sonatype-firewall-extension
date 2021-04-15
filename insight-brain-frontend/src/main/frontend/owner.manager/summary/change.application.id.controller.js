/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ChangeApplicationIdController(
  $scope,
  $rootScope,
  $state,
  owner,
  siblings,
  Messages,
  ApplicationStore,
  OwnerConstant,
  EventNameConstant
) {
  var vm = this;

  vm.isDirty = isDirty;
  vm.changeApplicationId = changeApplicationId;
  vm.originalApp = owner;
  vm.dirtyApp = ApplicationStore.create();
  vm.error = undefined;
  vm.siblings = siblings;
  vm.applicationIdEditorMask = undefined;
  vm.unsavedModalVisible = false;
  // Override messages to be used in the field validation popover
  const invalidCharactersMessage =
    'Use valid characters: alphanumeric, "_", "." or "-"';
  vm.formMessages = {
    duplicate: 'ID is already in use',
    validNameCharacters: invalidCharactersMessage,
    noSpaces: invalidCharactersMessage,
  };

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
    vm.applicationIdEditorMask.wrap(vm.originalApp.$save()).then(
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
    return (
      vm.dirtyApp.publicId !== null &&
      vm.dirtyApp.publicId !== vm.originalApp.publicId
    );
  }
}

ChangeApplicationIdController.$inject = [
  '$scope',
  '$rootScope',
  '$state',
  'owner',
  'siblings',
  'Messages',
  'ApplicationStore',
  'owner.constant',
  'event.name.constant',
];
