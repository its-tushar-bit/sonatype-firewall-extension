/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ApiPage from './ApiPage';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import { ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE } from 'MainRoot/utility/services/routeStateUtilService';

export default angular
  .module('apiModule', [])
  .component('apiPage', react2angular(withStoreProvider(ApiPage), [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('api', {
    url: '/api',
    data: {
      title: 'API',
      authenticationRequired: ROUTE_AUTHENTICATION_REQUIRED_BACKEND_CONFIGURABLE,
    },
    component: 'apiPage',
  });
}

routes.$inject = ['$stateProvider'];
