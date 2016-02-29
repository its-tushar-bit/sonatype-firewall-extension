/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function Sort() {
    return {
      restrict: 'A',
      controller: 'sort.controller',
      controllerAs: 'sortVm'
    };
  }

  angular.module('utility').directive('sort', Sort);

}(angular));
