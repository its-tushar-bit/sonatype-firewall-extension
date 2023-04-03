/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import BaseUrlConfiguration from './BaseUrlConfiguration';

const baseUrlConfigurationModule = angular
  .module('baseUrlConfiguration', [])
  .component('baseUrlConfiguration', iqReact2Angular(BaseUrlConfiguration, [], ['$ngRedux', '$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('baseUrlConfiguration', {
        url: '/baseUrl',
        component: 'baseUrlConfiguration',
        data: {
          title: 'Base URL Configuration',
          isDirty: ['baseUrlConfiguration', 'isDirty'],
        },
      });
    },
  ]);

export default baseUrlConfigurationModule;
