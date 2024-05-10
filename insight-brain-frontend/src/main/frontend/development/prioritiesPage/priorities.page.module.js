/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import PrioritiesPage from 'MainRoot/development/prioritiesPage/PrioritiesPage';

const prioritiesPageModule = angular
  .module('prioritiesPageModule', ['ngRedux'])
  .component('prioritiesPage', iqReact2Angular(PrioritiesPage, [], ['$ngRedux', '$state']))
  .config(routes);

const url = '/development/priorities/{publicAppId}/{scanId}';

function routes($stateProvider, $urlRouterProvider) {
  $stateProvider.state('prioritiesPage', {
    url,
    component: 'prioritiesPage',
    data: {
      title: 'Priorities',
    },
  });

  $urlRouterProvider.when(`${url}/`, url);
}

routes.$inject = ['$stateProvider', '$urlRouterProvider'];

export default prioritiesPageModule;
