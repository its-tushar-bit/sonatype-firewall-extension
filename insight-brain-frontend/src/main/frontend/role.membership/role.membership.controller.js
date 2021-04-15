/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit, map } from 'ramda';

export default function RoleMembershipController(
  $scope,
  $http,
  CLMContextLocations,
  Messages
) {
  var vm = this;

  vm.accessEditor = undefined;
  vm.accessEditorSearch = undefined;
  vm.accessEditorSearchMask = undefined;
  vm.searchInProgress = false;
  vm.loadError = undefined;
  vm.query = '';
  vm.searchError = undefined;
  vm.newGroupName = undefined;
  vm.addGroup = addGroup;
  vm.groupExists = groupExists;
  vm.search = search;
  vm.getIconName = getIconName;
  vm.getTooltip = getTooltip;
  vm.makeEditorPristine = makeEditorPristine;
  vm.isDirty = isDirty;

  /*
   * These three properties are lists (or functions that return lists) that keep track of the users that
   * are (or were) in the role.  Each list has a slightly different role:
   *
   * `originalMembers` List of users passed in from parent scope which defines the users who were in the role
   * to start off
   *
   * `members` Internal list representing all users currently present in either side of the picker.  This is what
   * is passed to the double-column-picker component
   *
   * `getCurrentMembers` A function that returns a filtered view of `members` that only shows the users who
   * are currently picked.  This property is exposed to the parent component.
   */
  vm.originalMembers = vm.originalMembers; //don't use undefined; it'll overwrite the passed-in value.
  vm.members = undefined;
  vm.getCurrentMembers = getCurrentMembers;
  vm.getCurrentMembersToSave = getCurrentMembersToSave;

  //initialize members and currentMembers and set up listeners to
  //keep them in sync
  $scope.$watch('vm.originalMembers', setMembers);

  $scope.$on('role.membership.makeEditorPristine', makeEditorPristine);

  //Update the currentMembers scope property based on the members property.  NOTE: This must be called
  //manually each time members or its contents are adjusted
  function getCurrentMembers() {
    return vm.members
      ? vm.members.filter(function (user) {
          return user.picked;
        })
      : [];
  }

  // Like getCurrentMembers, but filters out properties that the server isn't expecting
  function getCurrentMembersToSave() {
    return map(omit(['picked', 'checked']), getCurrentMembers());
  }

  function setMembers(originalMembers) {
    vm.members = originalMembers
      ? originalMembers.map(function (member) {
          var copy = angular.copy(member);
          copy.picked = true;
          copy.checked = false;
          return copy;
        })
      : [];
  }

  function addGroup() {
    var group = {
      displayName: vm.newGroupName,
      email: null,
      internalName: vm.newGroupName,
      type: 'GROUP',
    };
    updatePickedUsers([group]);

    vm.newGroupName = undefined;
    vm.accessEditorAddGroup.$setPristine();
  }

  function makeEditorPristine() {
    delete vm.newGroupName;
    delete vm.query;
    delete vm.searchError;
    vm.accessEditor.$setPristine();
    vm.accessEditorSearch.$setPristine();
    if (vm.accessEditorAddGroup) {
      vm.accessEditorAddGroup.$setPristine();
    }
  }

  function search() {
    if (vm.query) {
      delete vm.searchError;
      var pickedUsers = getCurrentMembers();
      vm.searchInProgress = true;

      vm.accessEditorSearchMask
        .wrap(
          $http.get(CLMContextLocations.getFindUsersUrl(), {
            params: {
              q: vm.query,
            },
          })
        )
        .then(
          function (result) {
            vm.searchInProgress = false;
            vm.members = result.data.members;
            updatePickedUsers(pickedUsers);

            if (result.data.error) {
              vm.searchError = result.data.error;
            }
          },
          function (error) {
            vm.searchInProgress = false;
            vm.searchError = Messages.getHttpErrorMessage(error);
          }
        );
    }
  }

  function updatePickedUsers(pickedUsers) {
    pickedUsers.forEach(function (pickedUser) {
      var replaced = vm.members.some(function (user, index) {
        if (
          user.internalName === pickedUser.internalName &&
          user.type === pickedUser.type
        ) {
          vm.members[index] = pickedUser;
          return true;
        }
      });
      if (!replaced) {
        vm.members.push(pickedUser);
      }
    });
  }

  function isDirty() {
    function getSortedNames(members) {
      return members
        .map(function (user) {
          return user.internalName;
        })
        .sort();
    }

    return !angular.equals(
      getSortedNames(getCurrentMembers()),
      getSortedNames(vm.originalMembers)
    );
  }

  function groupExists(groupName) {
    return vm.members.some(function (member) {
      return member.internalName === groupName;
    });
  }

  function getIconName(item) {
    return item.type === 'USER' ? 'fa-user' : 'fa-group';
  }

  function getTooltip(item) {
    return item.realm && item.type !== 'GROUP'
      ? item.realm + (item.email ? '\n' + item.email : '')
      : null;
  }
}

RoleMembershipController.$inject = [
  '$scope',
  '$http',
  'CLMContextLocations',
  'Messages',
];
