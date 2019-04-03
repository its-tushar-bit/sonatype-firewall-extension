/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function SelectApplicationContactController($scope, $http, CLMContextLocations, owner, DeleteModalService, Messages) {
  var vm = this;

  vm.deleteMode = false;
  vm.isDirty = isDirty;
  vm.owner = owner;
  vm.query = undefined;
  vm.removeContact = removeContact;
  vm.search = search;
  vm.searchError = undefined;
  vm.selected = undefined;
  vm.submitError = undefined;
  vm.updateContact = updateContact;
  vm.selectContactFormMask = undefined;
  vm.unsavedModalVisible = false;
  vm.users = undefined;

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.isDirty()) {
      vm.unsavedModalVisible = true;
      event.preventDefault();
    }
  });

  $scope.$on('pageChangeCanceled', function() {
    vm.unsavedModalVisible = false;
  });

  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });

  function search() {
    delete vm.searchError;
    delete vm.submitError;
    delete vm.selected;
    $http.get(CLMContextLocations.getFindUsersUrl(), {
      params: {
        q: vm.query,
        groups: false
      }
    }).then(function (result) {
      vm.users = result.data.members;
      if (vm.owner.contact) {
        vm.users.forEach(function(user) {
          if (user.internalName === vm.owner.contact.internalName) {
            vm.selected = user;
          }
        });
      }
    }, function (error) {
      vm.searchError = Messages.getHttpErrorMessage(error);
    });
  }

  function updateContact() {
    delete vm.submitError;
    owner.contactInternalName = vm.selected.internalName;
    vm.selectContactFormMask.wrap(owner.$save()).then(function() {
      $scope.$close();
    }, function(error) {
      vm.submitError = Messages.getHttpErrorMessage(error);
    });
  }

  function removeContact() {
    owner.contactInternalName = null;
    vm.deleteMode = true;
    DeleteModalService.deleteCustom('Clear Contact', 'You are about to remove ' +
        vm.owner.contact.displayName + '.', 'Removing', function() {
      return vm.owner.$save();
    }).then(function() {
      $scope.$close();
    }, function(error) {
      vm.error = error;
      vm.deleteMode = false;
    });
  }

  function isDirty() {
    if (!vm.selected) {
      return false;
    }
    else {
      return vm.owner.contact ? vm.selected.internalName !== vm.owner.contact.internalName : true;
    }
  }
}

SelectApplicationContactController.$inject = [
  '$scope', '$http', 'CLMContextLocations', 'owner', 'DeleteModalService', 'Messages'
];
