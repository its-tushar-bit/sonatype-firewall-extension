/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SubmitWrapperController(Messages) {
    var vm = this;
    vm.errorDetails = errorDetails;

    function errorDetails(error) {
      return Messages.getHttpErrorMessage(error);
    }
  }

  SubmitWrapperController.$inject = ['Messages'];

  angular.module('utility').controller('SubmitWrapperController', SubmitWrapperController);
}(angular));
