/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { actions } from './successMetricsReportSlice';
import SuccessMetricsReport from './SuccessMetricsReport.jsx';

export default connect(
  ({ successMetrics: { successMetricsReport }, router }) => ({
    ...successMetricsReport,
    router,
  }),
  { ...actions }
)(SuccessMetricsReport);
