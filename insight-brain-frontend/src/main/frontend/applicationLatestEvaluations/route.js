/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ApplicationLatestEvaluationsPage from 'MainRoot/applicationLatestEvaluations/ApplicationLatestEvaluationsPage';

router.stateRegistry.register({
  name: 'applicationLatestEvaluations',
  url: '/applicationLatestEvaluations/{applicationPublicId}/stage/{stageId}',
  component: ApplicationLatestEvaluationsPage,
  data: {
    title: 'Application Latest Evaluations',
  },
});
