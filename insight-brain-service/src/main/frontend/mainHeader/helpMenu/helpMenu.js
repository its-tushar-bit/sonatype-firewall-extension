/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmServerVersion */
function HelpMenuController() {
  var vm = this;
  vm.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');
}

angular.module('mainHeader').component('helpMenu', {
  controller: HelpMenuController,
  controllerAs: 'vm',
  templateUrl: 'mainHeader/helpMenu/helpMenu.html?' + clmBuildTimestamp
});
