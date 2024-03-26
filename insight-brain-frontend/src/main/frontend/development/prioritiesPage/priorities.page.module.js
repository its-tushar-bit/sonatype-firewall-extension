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

function routes($stateProvider) {
  $stateProvider.state('prioritiesPage', {
    url: '/development/priorities/{publicAppId}/{scanId}',
    component: 'prioritiesPage',
    data: {
      title: 'Priorities',
    },
  });
}

routes.$inject = ['$stateProvider'];

export default prioritiesPageModule;
