/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import SuccessMetricsReportListContainer from './successMetrics/SuccessMetricsReportListContainer';
import SuccessMetricsReportContainer from './successMetrics/successMetricsReport/SuccessMetricsReportContainer';
import LabsLayout from './LabsLayout';

// Labs abstract parent state
router.stateRegistry.register({
  name: 'labs',
  abstract: true,
  url: '/labs',
  component: LabsLayout,
});

router.stateRegistry.register({
  name: 'labs.successMetrics',
  url: '/successMetrics',
  component: SuccessMetricsReportListContainer,
  data: {
    title: 'Success Metrics',
  },
});

router.stateRegistry.register({
  name: 'labs.successMetricsReport',
  url: '/successMetrics/:successMetricsReportId',
  component: SuccessMetricsReportContainer,
  data: {
    title: 'Success Metrics Report',
  },
});
