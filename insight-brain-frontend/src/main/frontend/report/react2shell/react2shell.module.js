/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import React2ShellPage from 'MainRoot/report/react2shell/React2ShellPage';

const react2ShellModule = angular
  .module('react2ShellModule', ['ui.router'])
  .component('react2ShellPage', iqReact2Angular(React2ShellPage, [], ['$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('react2ShellReport', {
    url: '/reports/react2shell',
    component: 'react2ShellPage',
    data: {
      title: 'React2Shell Vulnerability Report',
    },
  });
}

routes.$inject = ['$stateProvider'];

export default react2ShellModule;
