/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function systemNoticeController(systemNoticeService, $scope) {
  var vm = this;

  vm.$onInit = function() {
    systemNoticeService.getSystemNotice().then(function(response) {
      vm.systemNotice = response;
    }).catch(function() {
      vm.systemNotice = systemNoticeService.getDefaultSystemNotice();
    });
  };

  $scope.$on('systemNoticeUpdated', function(systemNoticeUpdated, systemNotice) {
    vm.systemNotice = systemNotice;
  });
}

var systemNotice = {
  templateUrl: 'systemNotice/systemNotice.html?' + clmBuildTimestamp,
  controller: systemNoticeController,
  controllerAs: 'vm'
};

systemNoticeController.$inject = ['systemNoticeService', '$scope'];

angular.module('systemNoticeModule').component('systemNotice', systemNotice);
