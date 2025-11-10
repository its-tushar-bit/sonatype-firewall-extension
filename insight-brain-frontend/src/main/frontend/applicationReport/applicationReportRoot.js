/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { pick } from 'ramda';

export default {
  template: '<ui-view></ui-view>',
  controllerAs: 'vm',
  controller: ApplicationReportRootController,
};

function ApplicationReportRootController($state, $ngRedux, applicationReportActions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      const actions = pick(['setReportParameters'], applicationReportActions);
      const boundActions = {};
      Object.keys(actions).forEach((key) => {
        boundActions[key] = (...args) => $ngRedux.dispatch(actions[key](...args));
      });

      boundActions.setReportParameters(
        $state.params.publicId,
        $state.params.scanId,
        !!$state.params.unknownjs,
        !!$state.params.embeddable,
        $state.params.policyViolationId,
        $state.params.componentHash,
        $state.params.tabId,
        true
      );
    },
  });
}

ApplicationReportRootController.$inject = ['$state', '$ngRedux', 'applicationReportActions'];
