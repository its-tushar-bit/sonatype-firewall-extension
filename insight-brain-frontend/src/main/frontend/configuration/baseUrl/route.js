/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import BaseUrlConfiguration from './BaseUrlConfiguration';

router.stateRegistry.register({
  name: 'baseUrlConfiguration',
  url: '/baseUrl',
  component: BaseUrlConfiguration,
  data: {
    title: 'Base URL Configuration',
    isDirty: ['baseUrlConfiguration', 'isDirty'],
  },
});
