/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqToggleSwitch.html';

/**
 * "iq-toggle-switch" reusable directive
 * Attributes:
 *
 *
 * Example:
 *
 */
let iqToggleSwitch = {
  replace: true,
  bindings: {
    label: '@',
    labelClass: '@',
    isChecked: '=',
    isDisabled: '<',
    onClick: '&',
    isRequired: '<',
  },
  controller: IqToggleSwitchController,
  controllerAs: 'vm',
  template: template,
  transclude: true,
};

function IqToggleSwitchController() {
  let vm = this;

  vm.onClick = function onClick() {
    vm.isChecked = !vm.isChecked;
  };
}

IqToggleSwitchController.$inject = ['$scope', '$element', '$attrs'];

export default iqToggleSwitch;
