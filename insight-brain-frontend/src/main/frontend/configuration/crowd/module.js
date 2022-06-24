/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import AtlassianCrowdConfiguration from './AtlassianCrowdConfiguration';

const atlassianCrowdConfigurationModule = angular
  .module('atlassianCrowdConfiguration', ['ui.router'])
  .component('atlassianCrowdConfiguration', iqReact2Angular(AtlassianCrowdConfiguration, [], ['$ngRedux', '$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('atlassianCrowdConfiguration', {
        url: '/crowd',
        component: 'atlassianCrowdConfiguration',
        data: {
          title: 'Atlassian Crowd Configuration',
          isDirty: ['atlassianCrowdConfiguration', 'isDirty'],
        },
      });
    },
  ]);

export default atlassianCrowdConfigurationModule;
