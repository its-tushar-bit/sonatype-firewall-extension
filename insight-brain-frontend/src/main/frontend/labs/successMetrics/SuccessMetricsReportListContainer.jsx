/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { path } from 'ramda';
import { connect } from 'react-redux';
import * as SuccessMetricsReportListActions from './successMetricsReportListSlice';
import SuccessMetricsReportList from './SuccessMetricsReportList.jsx';

export default connect(path(['successMetrics', 'successMetricsList']), { ...SuccessMetricsReportListActions })(
  SuccessMetricsReportList
);
