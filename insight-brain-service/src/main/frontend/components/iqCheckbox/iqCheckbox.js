/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  /**
   * "iq-checkbox" reusable component
   * Attributes:
   * - inputId {string}: (optional) will be used as 'id' of checkbox input element
   * - label {string}: (optional)
   * - isChecked {boolean}
   * - isDisabled {boolean}
   * - onClick {expression}
   *
   * Example:
   * <iq-checkbox label="Subscribe" on-click="toggleSubscribe()" is-checked="isSubscribed" ></iq-checkbox>
   */
  angular.module('components').component('iqCheckbox', {
    bindings: {
      inputId: '@',
      label: '@',
      onClick: '&',
      isChecked: '<',
      isDisabled: '<'
    },
    controller: IqCheckboxController,
    controllerAs: 'vm',
    templateUrl: 'components/iqCheckbox/iqCheckbox.html?' + clmBuildTimestamp,
    transclude: true
  });

  function IqCheckboxController($scope) {
    var vm = this;

    vm.getId = getId;
    vm.hasLabel = hasLabel;

    function getId() {
      return vm.inputId || 'iq_checkbox_' + $scope.$id;
    }

    function hasLabel() {
      return vm.label && vm.label.length;
    }
  }

  IqCheckboxController.$inject = ['$scope'];

}());
