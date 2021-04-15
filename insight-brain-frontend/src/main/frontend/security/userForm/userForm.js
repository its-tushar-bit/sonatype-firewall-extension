/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './userForm.html';

/* global angular, AngularUtils */
export default {
  bindings: {
    user: '<?',
    existingUsers: '<',
    onSave: '&',
    onCancel: '&',
  },
  template,
  controllerAs: 'vm',
  controller: UserFormController,
};

const invalidCharactersMessage = 'Use valid characters: alphanumeric, "_", "." or "-"';

function UserFormController($scope, UserStore, Dialog) {
  var vm = this;

  vm.saveClick = saveClick;
  vm.cancelClick = cancelClick;

  // clone if existing user
  vm.user = vm.user && vm.user.id ? vm.user.$clone() : UserStore.create();
  vm.passwordValidate = '';

  vm.usernameMessages = {
    duplicate: 'Username already taken',
    pattern: invalidCharactersMessage,
    validNameCharacters: invalidCharactersMessage,
  };

  vm.identifier = Math.random();

  function isDirty() {
    return vm.user.isDirty();
  }

  function saveClick() {
    var user = vm.user;

    if (!vm.saving) {
      vm.alerts = null;
      vm.saving = true;

      user.$save().then(
        function () {
          vm.saving = false;

          // signal save to parent controller
          vm.onSave();
        },
        function (error) {
          vm.alerts = [AngularUtils.toAlert(error.data)];
          vm.saving = false;
        }
      );
    }
  }

  /**
   * Click handler for the cancel button.  This is mostly concerned with putting up a dialog warning the user
   * that they will lose their saved changes if they continue.  The usual global dialog for that cannot be relied on
   * here as it only fires on page changes, and this form is usable in inline situations where cancelling doesn't
   * necessarily result in a page change.  We do however need to make sure that we don't get both dialogs when
   * a page change does actually happen, which is why the model is reverted after the first dialog is dismissed
   */
  function cancelClick() {
    var user = vm.user;

    function doCancel() {
      user.$revert();
      vm.onCancel();
    }

    if (isDirty()) {
      Dialog.open({
        title: 'Unsaved Changes',
        body: 'The current user has unsaved changes, continuing will lose them.',
        id: 'dirty-user-confirmation',
        buttons: [
          {
            name: 'Continue',
            type: 'primary',
            click: doCancel,
          },
          {
            name: 'Cancel',
            type: 'cancel',
          },
        ],
      });
    } else {
      doCancel();
    }
  }

  // make sure user is aware they are about to lose changes
  $scope.$on('pageChangeStarted', function (event) {
    if (isDirty()) {
      event.preventDefault();
    }
  });
}

UserFormController.$inject = ['$scope', 'UserStore', 'Dialog'];
