/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ApiPage from './ApiPage';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';

export default angular
  .module('apiModule', [])
  .component('apiPage', react2angular(withStoreProvider(ApiPage), [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('api', {
    url: '/api',
    data: {
      title: 'API',
      authenticationRequired: false,
    },
    component: 'apiPage',
  });
}

routes.$inject = ['$stateProvider'];
