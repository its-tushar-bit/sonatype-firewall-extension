/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import LegalDashboardContainer from './LegalDashboardContainer';

export default angular
  .module('legalDashboardModule', [])
  .component(
    'legalDashboard',
    react2angular(
      withStoreProvider(LegalDashboardContainer),
      ['isAuthorized'],
      ['$ngRedux']
    )
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('legalDashboard', {
    url: '/legal/dashboard',
    component: 'legalDashboard',
    data: {
      title: 'Legal Dashboard',
    },
  });
}

routes.$inject = ['$stateProvider'];
