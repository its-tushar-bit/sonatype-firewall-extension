/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { combineReducers } from 'redux';
import addSuccessMetricsReportReducer from './addSuccessMetricsReport/addSuccessMetricsReportReducer';
import successMetricsReportReducer from './successMetricsReport/successMetricsReportSlice';
import { successMetricsListReducer } from 'MainRoot/labs/successMetrics/successMetricsReportListSlice';

export default combineReducers({
  successMetricsList: successMetricsListReducer,
  addSuccessMetricsReport: addSuccessMetricsReportReducer,
  successMetricsReport: successMetricsReportReducer,
});
