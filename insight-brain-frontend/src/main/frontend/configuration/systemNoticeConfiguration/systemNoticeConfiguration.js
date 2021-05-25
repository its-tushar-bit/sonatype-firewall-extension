/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './systemNoticeConfiguration.html';

var systemNoticeConfiguration = {
  controller: SystemNoticeConfigurationController,
  bindings: {
    isAuthorized: '<',
  },
  controllerAs: 'vm',
  template: template,
};

function SystemNoticeConfigurationController($rootScope, systemNoticeService) {
  var vm = this;
  vm.load = load;
  vm.save = save;
  vm.cancel = cancel;
  vm.isChanged = isChanged;

  vm.load();

  function load() {
    vm.error = undefined;
    vm.loaded = false;

    systemNoticeService
      .getSystemNotice()
      .then(function (response) {
        vm.savedSystemNotice = response;
        vm.systemNotice = angular.copy(vm.savedSystemNotice);
      })
      .catch(function (error) {
        vm.error = error;
        vm.savedSystemNotice = systemNoticeService.getDefaultSystemNotice();
        vm.systemNotice = angular.copy(vm.savedSystemNotice);
      })
      .finally(function () {
        vm.loaded = true;
      });
  }

  function save() {
    vm.error = undefined;
    systemNoticeService
      .saveSystemNotice(vm.systemNotice)
      .then(function (data) {
        vm.savedSystemNotice = data;
        $rootScope.$broadcast('systemNoticeUpdated', angular.copy(vm.savedSystemNotice));
      })
      .catch(function (error) {
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

SystemNoticeConfigurationController.$inject = ['$rootScope', 'systemNoticeService'];

export default systemNoticeConfiguration;
