/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular, window) {
  'use strict';

  function OwnerManagerController($state, commonCodeFactory) {
    var vm = this;

    vm.$state = $state;
    vm.syncAlerts = [];

    var error = commonCodeFactory.getEncodedQueryString('errorMessage');
    if (error) {
      vm.syncAlerts.push({ type: 'error', msg: window.decodeURIComponent(error) });
    }
  }
  OwnerManagerController.$inject = ['$state', 'commonCodeFactory'];

  angular //
      .module('owner.manager.module') //
      .controller('owner.manager.controller', OwnerManagerController);
}(angular, window));
