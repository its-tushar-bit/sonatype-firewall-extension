/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ColorPickerController() {
    var vm = this;

    vm.colorRows = [
      [ 'light-red', 'yellow', 'light-green', 'light-blue', 'light-purple' ],
      [ 'dark-red', 'orange', 'dark-green', 'dark-blue', 'dark-purple' ]
    ];
  }

  function ColorPicker() {
    return {
      scope: {
        ngModel: '='
      },
      templateUrl: 'utility/widgets/color.picker.directive.html',
      controller: ColorPickerController,
      controllerAs: 'vm',
      bindToController: true,
      require: 'ngModel'
    };
  }

  angular
      .module('utility')
      .directive('colorPicker', ColorPicker);
}(angular));
