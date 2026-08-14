/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ApplicationLatestEvaluationsPage from 'MainRoot/applicationLatestEvaluations/ApplicationLatestEvaluationsPage';

router.stateRegistry.register({
  name: 'applicationLatestEvaluations',
  // componentDisplayName forwards the friendly hosted-repo component name (CLM-42090).
  url:
    '/applicationLatestEvaluations/{applicationPublicId}/stage/{stageId}?scanId&origin&repositoryManagerId&repositoryId&repositoryPublicId&componentDisplayName',
  component: ApplicationLatestEvaluationsPage,
  data: {
    title: 'Application Latest Evaluations',
  },
});

// HRC latest evaluations route — reuses the same page component in HRC mode.
router.stateRegistry.register({
  name: 'hostedRepositoryComponentLatestEvaluations',
  url: '/hostedRepositoryComponentLatestEvaluations/{hrcId}/stage/{stageId}?scanId&componentDisplayName',
  component: ApplicationLatestEvaluationsPage,
  data: {
    title: 'HRC Latest Evaluations',
  },
});
