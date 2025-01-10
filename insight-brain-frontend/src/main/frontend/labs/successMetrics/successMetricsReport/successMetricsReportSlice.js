/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getApplicationsUrl,
  getSuccessMetricsChartDataUrl,
  getSuccessMetricsComponentCountsUrl,
  getSuccessMetricsConfigUrl,
  getSuccessMetricsReportsUrl,
  getSuccessMetricsReportUrl,
  getSuccessMetricsStageIdUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';

export const REDUCER_NAME = 'successMetricsReport';

export const SUCCESS_METRICS_DISABLED_MESSAGE = 'Success metrics have been disabled by your system administrator.';

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
  successMetricsStageId: null,
});

export const load = createAsyncThunk(
  `${REDUCER_NAME}/fetchSuccessReport`,
  async (successMetricsReportId, { rejectWithValue }) => {
    try {
      // check if the success metrics feature is enabled before making additional network calls
      const { data } = await axios.get(getSuccessMetricsConfigUrl());

      if (data.enabled) {
        return await loadReportData(successMetricsReportId);
      } else {
        return rejectWithValue(SUCCESS_METRICS_DISABLED_MESSAGE);
      }
    } catch (error) {
      return rejectWithValue(error.message);
    }
  }
);

const loadReportData = async (successMetricsReportId) => {
  const dataChartRequest = axios.get(getSuccessMetricsChartDataUrl(successMetricsReportId));
  const userReportsRequest = axios.get(getSuccessMetricsReportsUrl());
  const componentCountsRequest = axios.get(getSuccessMetricsComponentCountsUrl(successMetricsReportId));
  const successMetricsStageIdRequest = axios.get(getSuccessMetricsStageIdUrl());

  const [
    { data: chartData },
    { data: reports },
    { data: componentCounts },
    { data: successMetricsStageIdResp },
  ] = await Promise.all([dataChartRequest, userReportsRequest, componentCountsRequest, successMetricsStageIdRequest]);

  const report = reports.find((rep) => rep.id === successMetricsReportId);
  const reportName = report.name;
  const includeLatestData = report.includeLatestData;

  const isSingleApplicationReport =
    !!(report && report.scope.applicationIds && report.scope.applicationIds.length === 1) &&
    (!report.scope.organizationIds || report.scope.organizationIds.length === 0);

  if (isSingleApplicationReport) {
    const { data } = await axios.get(getApplicationsUrl());
    const application = data.find((app) => app.id === report.scope.applicationIds[0]);

    const singleApplicationName = application ? application.name : null;

    return {
      ...chartData,
      reportName,
      isSingleApplicationReport,
      includeLatestData,
      singleApplicationName,
      componentCounts,
      successMetricsStageId: successMetricsStageIdResp?.successMetricsStageId,
    };
  }

  return {
    ...chartData,
    reportName,
    isSingleApplicationReport,
    includeLatestData,
    componentCounts,
    successMetricsStageId: successMetricsStageIdResp?.successMetricsStageId,
  };
};

const loadReportFulfilled = (state, { payload }) => {
  return {
    ...state,
    ...payload,
    loading: false,
  };
};

const loadReportFailed = (state, { payload }) => {
  state.loading = false;
  state.successMetricsStageId = null;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

export const deleteReport = createAsyncThunk(
  `${REDUCER_NAME}/deleteReport`,
  async (successMetricsReportId, { rejectWithValue, dispatch }) => {
    try {
      await axios.delete(getSuccessMetricsReportUrl(successMetricsReportId));
      startDeleteMaskSuccessTimer(dispatch);
    } catch (error) {
      rejectWithValue(error.message);
    }
  }
);

const deleteReportPending = (state) => {
  state.deleteMaskState = false;
};

const deleteReportFulfilled = (state) => {
  state.deleteError = null;
  state.deleteMaskState = true;
};

const deleteReportRejected = (state, { payload }) => {
  state.deleteMaskState = null;
  state.deleteError = Messages.getHttpErrorMessage(payload);
};

const startDeleteMaskSuccessTimer = (dispatch) => {
  setTimeout(() => {
    dispatch(actions.deleteMaskTimerDone());
    dispatch(stateGo('labs.successMetrics'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const successMetricsReportSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    deleteMaskTimerDone: (state) => {
      return {
        ...state,
        deleteMaskState: null,
      };
    },
  },
  extraReducers: {
    [load.pending]: () => initialState,
    [load.fulfilled]: loadReportFulfilled,
    [load.rejected]: loadReportFailed,
    [deleteReport.pending]: deleteReportPending,
    [deleteReport.fulfilled]: deleteReportFulfilled,
    [deleteReport.rejected]: deleteReportRejected,
  },
});

export default successMetricsReportSlice.reducer;

export const actions = {
  ...successMetricsReportSlice.actions,
  load,
  deleteReport,
};
