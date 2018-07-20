/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import template from './iqRadio.html';

/**
 * "iq-radio" reusable component
 *
 * Attributes:
 * - model {expression}: Assignable AngularJS expression to data-bind to
 * - value {expression}: AngularJS expression to which ngModel will be be set when the radio is selected
 * - onClick {expression}: (optional) 'value' variable will be available to provided expression
 * - isDisabled {boolean}: (optional) default is false
 * - inputId {string}: (optional) will be used as 'id' of checkbox input element
 *
 * Example:
 * <iq-radio model="color.name" value="'red'"><em>Red</em></iq-radio>
 * <iq-radio model="color.name" value="'blue'"><em>Blue</em></iq-radio>
 */
var iqRadio = {
  bindings: {
    inputId: '@',
    isDisabled: '<',
    model: '=',
    value: '<',
    onClick: '&'
  },
  controller: IqRadioController,
  controllerAs: 'vm',
  template: template,
  transclude: true
};

function IqRadioController($scope) {
  var vm = this;
  vm.getId = getId;

  function getId() {
    return vm.inputId || 'iq_radio_' + $scope.$id;
  }
}

IqRadioController.$inject = ['$scope'];

export default iqRadio;
