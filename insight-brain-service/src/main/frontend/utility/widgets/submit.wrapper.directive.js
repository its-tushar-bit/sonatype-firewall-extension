/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Common handling of submit errors returned by the server.
 * Usage: assign the return value of any failed $http call to $scope.error and surround
 * the DOM content depending on the $http call with this directive.
 */
function SubmitWrapper() {

  return {
    restrict: 'A',
    priority: 99,
    transclude: true,
    replace: true,
    bindToController: true,
    controller: SubmitWrapperController,
    controllerAs: 'vm',
    templateUrl: 'utility/widgets/submit.wrapper.directive.html',
    scope: {
      error: '=submitWrapper',
      errorMessage: '=message',
      retry: '&'
    }
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

angular.module('utility').directive('submitWrapper', SubmitWrapper);
