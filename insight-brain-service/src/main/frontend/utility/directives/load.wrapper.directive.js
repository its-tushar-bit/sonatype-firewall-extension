/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  /**
   * Common handling of errors returned by the server.
   * Usage: assign the return value of any failed $http call to $scope.error and surround
   * the DOM content depending on the $http call with this directive.
   */
  function LoadWrapper() {
    return {
      restrict: 'A',
      priority: 99,
      transclude: true,
      replace: true,
      template: '<div class="iq-load-wrapper">' +
                  '<div class="iq-spinner__wrapper" ng-if="!vm.error && vm.isLoading()">' +
                    '<i class="fa fa-spin fa-circle-o-notch iq-spinner__icon"></i>Loading' +
                  '</div>' +
                  '<div ng-if="!vm.error && !vm.isLoading()">' + // ng-if is important for intial-value
                    '<div ng-transclude></div>' +
                  '</div>' +
                  '<div load-error="vm.error" reload="vm.reload()" message="vm.errorMessage"/>' +
                '</div>',
      scope: {
        error: '=loadWrapper',
        errorMessage: '=message',
        loading: '=?',
        reload: '&'
      },
      controller: LoadWrapperController,
      controllerAs: 'vm',
      bindToController: true
    };
  }

  function LoadWrapperController(Messages) {
    var vm = this;

    vm.isLoading = isLoading;
    vm.getDetails = getDetails;

    function isLoading() {
      return vm.loading;
    }

    function getDetails() {
      return Messages.getHttpErrorMessage(vm.error);
    }
  }

  LoadWrapperController.$inject = ['Messages'];

  angular //
      .module('utility.directives') //
      .directive('loadWrapper', LoadWrapper);

}(angular));
