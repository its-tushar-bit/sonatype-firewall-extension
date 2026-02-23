/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import AtlassianCrowdConfiguration from './AtlassianCrowdConfiguration';

router.stateRegistry.register({
  name: 'atlassianCrowdConfiguration',
  url: '/crowd',
  component: AtlassianCrowdConfiguration,
  data: {
    title: 'Atlassian Crowd Configuration',
    isDirty: ['atlassianCrowdConfiguration', 'isDirty'],
  },
});
