/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function SystemConfigurationMenuController($state) {
    var vm = this;
    vm.state = $state;
  }

  SystemConfigurationMenuController.$inject = ['$state'];

  angular.module('mainHeader').component('systemConfigurationMenu', {
    templateUrl: 'mainHeader/systemConfigurationMenu/systemConfigurationMenu.html?' + clmBuildTimestamp,
    controller: SystemConfigurationMenuController,
    controllerAs: 'vm',
    bindings: {
      permissions: '<'
    }
  });

}());
