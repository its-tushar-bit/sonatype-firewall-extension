/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function systemNoticeConfigurationController(isAuthorized, systemNoticeService, $rootScope) {
  var vm = this;
  vm.isAuthorized = isAuthorized;
  vm.load = load;
  vm.save = save;
  vm.cancel = cancel;
  vm.isChanged = isChanged;

  vm.load();

  function load() {
    vm.error = undefined;
    vm.loaded = false;

    systemNoticeService.getSystemNotice().then(function(response) {
      vm.savedSystemNotice = response;
      vm.systemNotice = angular.copy(vm.savedSystemNotice);
    }).catch(function(error) {
      vm.error = error;
      vm.savedSystemNotice = systemNoticeService.getDefaultSystemNotice();
      vm.systemNotice = angular.copy(vm.savedSystemNotice);
    }).finally(function() {
      vm.loaded = true;
    });
  }

  function save() {
    vm.error = undefined;
    systemNoticeService.saveSystemNotice(vm.systemNotice).then(function() {
      vm.savedSystemNotice = angular.copy(vm.systemNotice);
      $rootScope.$broadcast('systemNoticeUpdated', angular.copy(vm.systemNotice));
    }).catch(function(error) {
      vm.error = error;
    });
  }

  function cancel() {
    vm.systemNotice = angular.copy(vm.savedSystemNotice);
  }

  function isChanged() {
    return !angular.equals(vm.savedSystemNotice, vm.systemNotice);
  }
}

systemNoticeConfigurationController.$inject = ['isAuthorized', 'systemNoticeService', '$rootScope'];

angular.module('systemNoticeConfigurationModule').controller('systemNoticeConfigurationController',
    systemNoticeConfigurationController);
