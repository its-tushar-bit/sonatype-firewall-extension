/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import React2ShellPage from 'MainRoot/report/react2shell/React2ShellPage';

router.stateRegistry.register({
  name: 'react2ShellReport',
  url: '/reports/react2shell',
  component: React2ShellPage,
  data: {
    title: 'React2Shell Vulnerability Report',
  },
});
