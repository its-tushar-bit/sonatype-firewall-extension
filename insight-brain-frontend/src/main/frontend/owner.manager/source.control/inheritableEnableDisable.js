/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './inheritableEnableDisable.html';

/**
 * "inheritable-enable-disable" reusable component
 *
 * Attributes:
 * - model {expression}: Assignable AngularJS expression to data-bind to
 * - value {expression}: AngularJS expression to which ngModel will be be set when the radio is selected
 * - onClick {expression}: (optional) 'value' variable will be available to provided expression
 * - isDisabled {boolean}: (optional) default is false
 * - inputId {string}: (optional) will be used as 'id' of checkbox input element
 */
var inheritableEnableDisable = {
  bindings: {
    id: '@',
    title: '@',
    description: '@',
    isAvailable: '<',
    isDisabled: '<',
    isRequired: '<',
    canInherit: '<',
    model: '=',
    inheritLabel: '<',
    unavailableMsg: '<',
  },
  controller: angular.noop,
  controllerAs: 'vm',
  template: template,
  transclude: true,
};

export default inheritableEnableDisable;
