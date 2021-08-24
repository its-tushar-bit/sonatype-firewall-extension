/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { lensProp, merge, set } from 'ramda';
import { createReducerFromActionMap, propSetConst } from '../../../util/reduxUtil';
import {
  SUCCESS_METRICS_REPORT_LOAD_REQUESTED,
  SUCCESS_METRICS_REPORT_LOAD_FAILED,
  SUCCESS_METRICS_REPORT_LOAD_FULFILLED,
  SUCCESS_METRICS_REPORT_DELETE_REQUESTED,
  SUCCESS_METRICS_REPORT_DELETE_FULFILLED,
  SUCCESS_METRICS_REPORT_DELETE_FAILED,
  SUCCESS_METRICS_DELETE_MASK_TIMER_DONE,
} from './SuccessMetricsReportActions';

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  deleteMaskState: null,
  deleteError: null,
  mttrs: [],
  averages: {},
  applicationCounts: {},
  violationCounts: [],
  violationsByCategoryWeeks: [],
  lastUpdated: null,
  monthCount: null,
  reportName: '',
  isSingleApplicationReport: null,
  singleApplicationName: null,
  includeLatestData: null,
  componentCounts: null,
});

const loadFulfilled = (payload, state) => {
  return {
    ...state,
    ...payload,
    loading: false,
  };
};

const reducerActionMap = {
  [SUCCESS_METRICS_REPORT_LOAD_REQUESTED]: () => initialState,
  [SUCCESS_METRICS_REPORT_LOAD_FAILED]: (payload, state) => set(lensProp('loadError'), payload, state),
  [SUCCESS_METRICS_REPORT_LOAD_FULFILLED]: loadFulfilled,
  [SUCCESS_METRICS_REPORT_DELETE_REQUESTED]: propSetConst('deleteMaskState', false),
  [SUCCESS_METRICS_REPORT_DELETE_FULFILLED]: (_, state) => merge(state, { deleteMaskState: true, deleteError: null }),
  [SUCCESS_METRICS_REPORT_DELETE_FAILED]: (payload, state) =>
    merge(state, { deleteMaskState: null, deleteError: payload }),
  [SUCCESS_METRICS_DELETE_MASK_TIMER_DONE]: propSetConst('deleteMaskState', null),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
