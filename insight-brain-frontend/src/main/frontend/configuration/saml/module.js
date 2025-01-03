/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocationModule from '../../utilAngular/CLMContextLocation';
import utilityModule from '../../utility/utility.module';
import SAMLConfigurationPage from './SAMLConfigurationPage';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

export default angular
  .module('samlModule', [clmContextLocationModule.name, utilityModule.name])
  .component('samlConfigurationPage', iqReact2Angular(SAMLConfigurationPage, [], ['$ngRedux', '$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('saml', {
        url: '/saml',
        component: 'samlConfigurationPage',
        data: {
          title: 'SAML',
          isDirty: ['samlConfiguration', 'isDirty'],
        },
      });
    },
  ]);
