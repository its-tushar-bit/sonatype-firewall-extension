/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './ownerPolicyList.html';

export default {
  template,
  controller: ownerPolicyListController,
  controllerAs: 'vm',
  bindings: {
    ownerPolicyList: '<',
    actionStages: '<',
    onPolicyClick: '&',
    isEnforcementSupportedForStage: '&',
  },
};

function ownerPolicyListController() {
  const vm = this;

  // initial sort order
  vm.sortFields = ['-threatLevel'];
}
