/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { identity } from 'ramda';
import { getDaysFromNow } from '../../../util/jsUtil';
import template from './productLicenseSummary.html';

export default {
  controllerAs: 'vm',
  template,
  controller: ProductLicenseSummaryController,
  bindings: {
    license: '<',
  },
};

const mkLimit = (name, count) => ({ name, count });

function ProductLicenseSummaryController() {
  const vm = this;

  Object.assign(vm, {
    daysToExpiration: undefined,
    userLimits: undefined,
    shouldDisplayApplicationLimit: undefined,

    $onInit() {
      vm.daysToExpiration = getDaysFromNow(vm.license.expiryTimestamp);
      vm.shouldDisplayApplicationLimit = vm.license.applicationLimitToDisplay != null;
      vm.userLimits = [
        vm.license.licensedUsersToDisplay && mkLimit('Lifecycle', vm.license.licensedUsersToDisplay),
        vm.license.firewallUsersToDisplay && mkLimit('Firewall', vm.license.firewallUsersToDisplay),
      ].filter(identity);
    },
  });
}
