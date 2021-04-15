/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './rawLicenseDisplay.html';
import { getDeclaredLicensesDisplay, getObservedLicensesDisplay } from '../../licenseDisplayUtils';

export default {
  template,
  controller: rawLicenseDisplayController,
  controllerAs: 'vm',
  bindings: {
    license: '<',
  },
};

function rawLicenseDisplayController() {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      if (vm.license) {
        vm.declaredLicenses = getDeclaredLicensesDisplay(vm.license);
        vm.observedLicenses = getObservedLicensesDisplay(vm.license);
      }
    },
  });
}
