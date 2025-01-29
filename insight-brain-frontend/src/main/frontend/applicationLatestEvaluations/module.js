/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ApplicationLatestEvaluationsPage from 'MainRoot/applicationLatestEvaluations/ApplicationLatestEvaluationsPage';

export default angular
  .module('applicationLatestEvaluationsModule', [])
  .component(
    'applicationLatestEvaluationsPage',
    iqReact2Angular(ApplicationLatestEvaluationsPage, [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('applicationLatestEvaluations', {
    url: '/applicationLatestEvaluations/{applicationPublicId}/stage/{stageId}',
    data: {
      title: 'Application Latest Evaluations',
    },
    component: 'applicationLatestEvaluationsPage',
  });
}

routes.$inject = ['$stateProvider'];
