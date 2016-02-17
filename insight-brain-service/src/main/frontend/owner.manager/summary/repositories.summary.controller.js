/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function RepositoriesSummaryController() {
    var vm = this;

    vm.error = undefined;
    vm.repositories = undefined;
    vm.doLoad = doLoad;

    vm.doLoad();

    function doLoad() {
      vm.repositories = [];
    }

  }

  angular//
      .module('owner.manager.module')//
      .controller('repositories.summary.controller', RepositoriesSummaryController);

}(angular));
