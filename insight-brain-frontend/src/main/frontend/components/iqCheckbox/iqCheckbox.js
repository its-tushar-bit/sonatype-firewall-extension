/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqCheckbox.html';

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
var iqCheckbox = {
  bindings: {
    label: '@',
    onClick: '&',
    isChecked: '<',
    isDisabled: '<',
  },
  controller: IqCheckboxController,
  controllerAs: 'vm',
  template: template,
  transclude: true,
};

function IqCheckboxController() {
  var vm = this;

  vm.hasLabel = hasLabel;

  function hasLabel() {
    return vm.label && vm.label.length;
  }
}

export default iqCheckbox;
