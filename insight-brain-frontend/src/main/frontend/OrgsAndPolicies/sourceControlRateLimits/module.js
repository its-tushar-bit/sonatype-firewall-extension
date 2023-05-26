/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import SourceControlRateLimits from './SourceControlRateLimits';

const sourceControlRateLimitsModule = angular
  .module('sourceControlRateLimits', ['ui.router'])
  .component('sourceControlRateLimits', iqReact2Angular(SourceControlRateLimits, [], ['$ngRedux', '$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('sourceControlRateLimits', {
        url: '/management/view/{ownerType}/{ownerId}/source-control-rate-limits',
        component: 'sourceControlRateLimits',
        data: {
          title: 'Source Control Rate Limits',
        },
      });
    },
  ]);

export default sourceControlRateLimitsModule;
