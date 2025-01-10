/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAction, createAsyncThunk, createReducer } from '@reduxjs/toolkit';
import axios from 'axios';
import { merge } from 'ramda';
import { getSuccessMetricsConfigUrl, getSuccessMetricsReportsUrl } from '../../util/CLMLocation';

const REDUCER_NAME = 'successMetricsList';
export const SUCCESS_METRICS_DISABLED_MESSAGE = 'Success metrics have been disabled by your system administrator.';

export const initialState = {
  loading: true,
  loadError: null,
  reports: [],
  isAddModalOpen: false,
};

export const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) => {
  return axios
    .get(getSuccessMetricsConfigUrl())
    .then(({ data: { enabled } }) => {
      if (!enabled) return rejectWithValue(SUCCESS_METRICS_DISABLED_MESSAGE);

      return axios
        .get(getSuccessMetricsReportsUrl())
        .then(({ data }) => data)
        .catch(rejectWithValue);
    })
    .catch(rejectWithValue);
});

export const newReport = createAction(`${REDUCER_NAME}/newReport`);
export const toggleAddModal = createAction(`${REDUCER_NAME}/toggleAddModal`);

export const successMetricsListReducer = createReducer(initialState, {
  [load.pending]: (state) => merge(state, { loading: true }),
  [load.fulfilled]: (state, { payload }) => merge(state, { reports: payload, loading: false, loadError: null }),
  [load.rejected]: (state, { payload }) => merge(state, { loading: false, loadError: payload }),
  [newReport.type]: (state, { payload }) => {
    state.reports.push(payload);
    state.isAddModalOpen = false;
  },
  [toggleAddModal.type]: (state) => {
    state.isAddModalOpen = !state.isAddModalOpen;
  },
});
