/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import OidcConfigurationPage from './OidcConfigurationPage';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import { selectIsDirty } from './oidcConfigurationSelectors';

export default angular
  .module('oidcModule', [])
  .component('oidcConfigurationPage', iqReact2Angular(OidcConfigurationPage, [], ['$ngRedux', '$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('oidc', {
        url: '/oidc',
        component: 'oidcConfigurationPage',
        data: {
          title: 'OIDC',
          isDirty: selectIsDirty,
        },
      });
    },
  ]);
