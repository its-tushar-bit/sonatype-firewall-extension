/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from './reactAdapter/StoreProvider';
import ReportsPage from './report/react/ReportsPage';
import withRouterStateProvider from 'MainRoot/reactAdapter/RouterStateProvider';

export default angular
  .module('ReportModule', ['ui.router'])
  .component(
    'reportsPage',
    react2angular(withStoreProvider(withRouterStateProvider(ReportsPage)), [], ['$ngRedux', '$state'])
  )
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('violations', {
        url: '/reports/violations',
        component: 'reportsPage',
        data: {
          title: 'Reports',
        },
      });
    },
  ]);
