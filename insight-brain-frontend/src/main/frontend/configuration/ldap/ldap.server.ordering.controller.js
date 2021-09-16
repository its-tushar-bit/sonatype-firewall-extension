/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './ldap.server.ordering.modal.html';

export function LdapServerOrderingController($scope, $http, LdapConfigurationStore, CLMLocation, Messages) {
  var vm = this,
    originalStoreOrder;

  vm.store = undefined;

  vm.cancel = cancel;
  vm.isDirty = isDirty;
  vm.moveDown = moveDown;
  vm.moveToFirst = moveToFirst;
  vm.moveToLast = moveToLast;
  vm.moveUp = moveUp;
  vm.save = save;

  // The store should always be loaded at this point
  LdapConfigurationStore.get().then(function (store) {
    vm.store = store.slice();
    sort();
    originalStoreOrder = vm.store.slice();
  });

  function cancel() {
    vm.store.forEach(function (resource) {
      resource.$revert();
    });
    $scope.$dismiss();
  }

  function isDirty() {
    return vm.store.some(function (server, index) {
      return server !== originalStoreOrder[index];
    });
  }

  function moveDown(ldapServer) {
    var index = vm.store.indexOf(ldapServer);
    if (index !== vm.store.length - 1) {
      swap(ldapServer, vm.store[index + 1]);
      scroll(ldapServer);
    }
  }

  function moveToLast(ldapServer) {
    ldapServer.priority = Number.MAX_VALUE;
    sort();
    updatePriority();
    scroll(ldapServer);
  }

  function moveToFirst(ldapServer) {
    ldapServer.priority = -Number.MAX_VALUE;
    sort();
    updatePriority();
    scroll(ldapServer);
  }

  function moveUp(ldapServer) {
    var index = vm.store.indexOf(ldapServer);
    if (index !== 0) {
      swap(ldapServer, vm.store[index - 1]);
      scroll(ldapServer);
    }
  }

  function save() {
    if (!isDirty()) {
      return;
    }
    var serverIds = vm.store.map(function (server) {
      return server.id;
    });

    vm.ldapOrderForm.wrap($http.put(CLMLocation.getLdapPriority(), serverIds)).then(
      function () {
        $scope.$close();
      },
      function (error) {
        vm.error = Messages.getHttpErrorMessage(error);
      }
    );
  }

  function scroll(ldapServer) {
    var container = angular.element('#ldap-server-ordering-modal .simple-list'),
      containerOffset = container.offset(),
      element = container.find('li:nth-child(' + (vm.store.indexOf(ldapServer) + 1) + ')'),
      elementOffset = element.offset();

    if (
      elementOffset.top < containerOffset.top ||
      elementOffset.top + element.outerHeight() > containerOffset.top + container.outerHeight()
    ) {
      element[0].scrollIntoView();
    }
  }

  function swap(ldapServerOne, ldapServerTwo) {
    var otherPriority = ldapServerOne.priority;
    ldapServerOne.priority = ldapServerTwo.priority;
    ldapServerTwo.priority = otherPriority;
    sort();
  }

  function sort() {
    vm.store.sort(function (a, b) {
      return a.priority - b.priority;
    });
  }

  function updatePriority() {
    vm.store.forEach(function (server, index) {
      server.priority = index + 1;
    });
  }
}

LdapServerOrderingController.$inject = ['$scope', '$http', 'LdapConfigurationStore', 'CLMLocations', 'Messages'];

export function LdapServerOrderingModal(Modal) {
  return {
    open: function () {
      return Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: LdapServerOrderingController,
        controllerAs: 'vm',
        template,
      }).result;
    },
  };
}
LdapServerOrderingModal.$inject = ['Modal'];
