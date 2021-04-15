/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './gettingStartedDocLink.html';
import { LINK_CLICKED_ACTION } from '../gettingStartedUsageTelemetryService';

export default {
  template,
  controllerAs: 'vm',
  controller: GettingStartedDocLinkController,
  bindings: {
    href: '@',
    linkText: '@',
  },
};

function GettingStartedDocLinkController(gettingStartedUsageTelemetryService) {
  const vm = this;

  Object.assign(vm, {
    onClick() {
      gettingStartedUsageTelemetryService.submitData(LINK_CLICKED_ACTION, {
        href: vm.href,
      });
    },
  });
}

GettingStartedDocLinkController.$inject = ['gettingStartedUsageTelemetryService'];
