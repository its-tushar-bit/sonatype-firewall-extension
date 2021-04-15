/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './submit.wrapper.directive.html';

/**
 * Common handling of submit errors returned by the server.
 * Usage: assign the return value of any failed $http call to $scope.error and surround
 * the DOM content depending on the $http call with this directive.
 */
export default function SubmitWrapper() {
  return {
    restrict: 'A',
    priority: 99,
    transclude: true,
    replace: true,
    bindToController: true,
    controller: SubmitWrapperController,
    controllerAs: 'vm',
    template,
    scope: {
      error: '=submitWrapper',
      errorMessage: '=message',
      retry: '&',
    },
  };
}

function SubmitWrapperController(Messages) {
  var vm = this;
  vm.errorDetails = errorDetails;

  function errorDetails(error) {
    return Messages.getHttpErrorMessage(error);
  }
}

SubmitWrapperController.$inject = ['Messages'];
