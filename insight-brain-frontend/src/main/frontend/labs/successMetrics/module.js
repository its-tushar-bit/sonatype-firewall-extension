/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import SuccessMetricsReportContainer from './successMetricsReport/SuccessMetricsReportContainer';
import SuccessMetricsReportListContainer from './SuccessMetricsReportListContainer';

export default angular
  .module('successMetricsModule', [])
  .component('successMetricsReport', iqReact2Angular(SuccessMetricsReportContainer, [], ['$ngRedux', '$state']))
  .component(
    'successMetricsReportList',
    iqReact2Angular(SuccessMetricsReportListContainer, [], ['$ngRedux', '$state'])
  );
