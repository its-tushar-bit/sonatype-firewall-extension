/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import { getActionStageUrl, getApplicationSummariesUrl, getApplicationSummaryUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { prop, propEq } from 'ramda';
import { debounce } from 'debounce';

const REDUCER_NAME = 'reports';
export const RESULTS_PER_PAGE = 50;
const FILTER_DEBOUNCE_TIME = 500;
const SORT_OPTIONS = {
  name: 'APP_NAME_ASC',
  '-name': 'APP_NAME_DESC',
  organizationName: 'ORG_NAME_ASC',
  '-organizationName': 'ORG_NAME_DESC',
};

export const initialState = {
  stages: [],
  loading: false,
  loadError: null,
  applicationsInformationList: [],
  appFilter: '',
  pages: 1,
  hasMoreResults: true,
  appliedSort: null,
  loadingPublicIds: new Set(),
};

function getContactInAppListByPublicId(applicationList = [], publicId = '') {
  return applicationList.find(propEq('publicId', publicId)).contact;
}

const loadStagesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadStagesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.stages = payload;
};

const loadStagesFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadStages = createAsyncThunk(`${REDUCER_NAME}/loadStages`, (_, { rejectWithValue }) => {
  return axios
    .get(getActionStageUrl())
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

const loadReportsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.hasMoreResults = true;
};

const loadReportsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;

  if (state.pages === 1) {
    state.applicationsInformationList.length = 0;
  }

  state.contactInfo = initialState.contactInfo;
  state.applicationsInformationList = state.applicationsInformationList.concat(payload);
  // we reach the end when the returned payload is less than we requested
  state.hasMoreResults = payload?.length === RESULTS_PER_PAGE;
};

const loadReportsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadReports = createAsyncThunk(`${REDUCER_NAME}/loadReports`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const pages = state.reports.pages;

  return axios
    .get(
      getApplicationSummariesUrl(
        state.reports.appFilter,
        SORT_OPTIONS[state.reports.appliedSort] || SORT_OPTIONS.name,
        pages,
        RESULTS_PER_PAGE
      )
    )
    .then(prop('data'))
    .catch(rejectWithValue);
});

const loadStagesAndReportsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.applicationsInformationList = [];
  state.pages = 1;
};

const loadStagesAndReportsFulfilled = (state) => {
  state.loading = false;
  state.loadError = null;
};

const loadStagesAndReportsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadStagesAndReports = createAsyncThunk(
  `${REDUCER_NAME}/loadStagesAndReports`,
  (_, { rejectWithValue, dispatch }) => {
    const promises = [dispatch(loadStages()).then(unwrapResult), dispatch(loadReports()).then(unwrapResult)];
    return Promise.all(promises).catch(rejectWithValue);
  }
);

const loadContactNamePending = (state, { meta: { arg: publicId } }) => {
  state.loadingPublicIds.add(publicId);
  getContactInAppListByPublicId(state.applicationsInformationList, publicId).error = false;
};

const loadContactNameFulfilled = (state, { payload, meta: { arg: publicId } }) => {
  if (payload?.contact?.displayName) {
    getContactInAppListByPublicId(state.applicationsInformationList, publicId).displayName =
      payload.contact.displayName;
  }
  if (payload?.contact?.error) {
    getContactInAppListByPublicId(state.applicationsInformationList, publicId).error = true;
  }

  state.loadingPublicIds.delete(publicId);
};

const loadContactNameRejected = (state, { meta: { arg: publicId } }) => {
  getContactInAppListByPublicId(state.applicationsInformationList, publicId).error = true;
  state.loadingPublicIds.delete(publicId);
};

const loadContactName = createAsyncThunk(`${REDUCER_NAME}/loadContactName`, (appPublicId, { rejectWithValue }) =>
  axios.get(getApplicationSummaryUrl(appPublicId)).then(prop('data')).catch(rejectWithValue)
);

function incrementPages(state) {
  state.pages = state.pages + 1;
}

function sortReports(state, { payload = 'name' }) {
  state.loading = true;
  state.pages = 1;
  state.appliedSort = payload;
}

function setFilter(state, { payload = '' }) {
  state.appFilter = payload;
  state.pages = 1;
}

const loadMore = () => {
  return (dispatch) => {
    dispatch(actions.incrementPages());
    dispatch(actions.loadReports());
  };
};

const filterReportsDebounce = debounce((dispatch) => {
  dispatch(actions.loadReports());
}, FILTER_DEBOUNCE_TIME);

const filterReports = (value) => (dispatch) => {
  dispatch(actions.setFilter(value));
  filterReportsDebounce(dispatch, value);
};

const reportsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: { incrementPages, sortReports, setFilter },
  extraReducers: {
    [loadStages.pending]: loadStagesRequested,
    [loadStages.fulfilled]: loadStagesFulfilled,
    [loadStages.rejected]: loadStagesFailed,
    [loadStagesAndReports.pending]: loadStagesAndReportsRequested,
    [loadStagesAndReports.fulfilled]: loadStagesAndReportsFulfilled,
    [loadStagesAndReports.rejected]: loadStagesAndReportsFailed,
    [loadReports.pending]: loadReportsRequested,
    [loadReports.fulfilled]: loadReportsFulfilled,
    [loadReports.rejected]: loadReportsFailed,
    [loadContactName.pending]: loadContactNamePending,
    [loadContactName.fulfilled]: loadContactNameFulfilled,
    [loadContactName.rejected]: loadContactNameRejected,
  },
});

export default reportsSlice.reducer;

export const actions = {
  ...reportsSlice.actions,
  filterReports,
  loadReports,
  loadStagesAndReports,
  loadMore,
  loadContactName,
};
