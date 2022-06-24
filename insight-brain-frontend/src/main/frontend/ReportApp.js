/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ReportsPage from './report/react/ReportsPage';

export default angular
  .module('ReportModule', ['ui.router'])
  .component('reportsPage', iqReact2Angular(ReportsPage, [], ['$ngRedux', '$state']))
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
