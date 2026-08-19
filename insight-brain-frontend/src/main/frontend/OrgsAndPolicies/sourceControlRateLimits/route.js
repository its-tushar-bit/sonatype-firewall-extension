/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import SourceControlRateLimits from './SourceControlRateLimits';

router.stateRegistry.register({
  name: 'sourceControlRateLimits',
  url: '/management/view/{ownerType}/{ownerId}/source-control-rate-limits',
  component: SourceControlRateLimits,
  data: {
    title: 'Source Control Rate Limits',
  },
});
