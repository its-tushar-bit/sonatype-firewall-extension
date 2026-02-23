/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ReportsPage from './react/ReportsPage';

router.stateRegistry.register({
  name: 'violations',
  url: '/reports/violations',
  component: ReportsPage,
  data: {
    title: 'Reports',
    viewportSized: true,
  },
});
