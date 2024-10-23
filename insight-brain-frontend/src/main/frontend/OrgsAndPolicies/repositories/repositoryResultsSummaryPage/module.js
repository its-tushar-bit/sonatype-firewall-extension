/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import RepositoryResultsSummaryPage from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/RepositoryResultsSummaryPage';

var reportModule = angular
  .module(
    'Report',
    ['ui.router'],
    [
      '$stateProvider',
      function ($stateProvider) {
        $stateProvider.state('repository-report', {
          url: '/repository/{repositoryId}/result?hideBackButton={hideButton}',
          component: 'repositoryResultsSummaryPage',
        });
      },
    ]
  )
  .component('repositoryResultsSummaryPage', iqReact2Angular(RepositoryResultsSummaryPage, [], ['$ngRedux', '$state']));

export default reportModule;
