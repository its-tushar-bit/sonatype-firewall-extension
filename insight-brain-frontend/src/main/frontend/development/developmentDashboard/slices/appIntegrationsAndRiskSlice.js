/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getAppIntegrationsAndRisk } from 'MainRoot/util/CLMLocation';
import {
  selectCurrentPage,
  selectPageSize,
  selectSort,
  selectNameFilter,
  selectScmFilter,
  selectCiCdFilter,
} from 'MainRoot/development/developmentDashboard/selectors/appIntegrationsAndRiskSelectors';

export const APP_INTEGRATIONS_AND_RISK_REDUCER_NAME = 'appIntegrationsAndRisk';

const PAGE_SIZE = 10;

export const COLUMNS = {
  NAME: 'NAME',
  COMMIT: 'COMMIT',
  EVALUATION: 'EVALUATION',
  TOTAL_RISK: 'TOTAL_RISK',
};

const loadAppIntegrationsAndRiskRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAppIntegrationsAndRiskFulfilled = (state, { payload }) => {
  const pageCount = Math.ceil(payload.total / PAGE_SIZE);

  state.loading = false;
  state.loadError = null;
  state.tableData = payload.results;
  state.pageCount = pageCount;
};

const loadAppIntegrationsAndRiskFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

function setCurrentPage(state, { payload }) {
  state.currentPage = payload.currentPage;
}

const setSort = (state, { payload }) => {
  state.sort = state.sort.includes('-') ? payload : `-${payload}`;
};

const setNameFilter = (state, { payload }) => {
  state.nameFilter = payload;
};

const setCiCdFilter = (state, { payload }) => {
  state.ciCdFilter = payload;
};

const setSCMFilter = (state, { payload }) => {
  state.scmFilter = payload;
};

const toggleFilterSideBar = (state, { payload }) => {
  state.showFilterSideBar = payload;
};

const loadAppIntegrationsAndRisk = createAsyncThunk(
  `${APP_INTEGRATIONS_AND_RISK_REDUCER_NAME}/loadAppIntegrationsAndRisk`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();

    return axios
      .get(getAppIntegrationsAndRisk(), {
        params: {
          page: selectCurrentPage(state) + 1,
          pageSize: selectPageSize(state),
          optionalOrderBy: selectSort(state),
          optionalFilterApplicationNamesBy: selectNameFilter(state),
          optionalFilterScmIsIntegrated: selectScmFilter(state),
          optionalFilterCiCdIsIntegrated: selectCiCdFilter(state),
        },
      })
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const appIntegrationsAndRiskSlice = createSlice({
  name: APP_INTEGRATIONS_AND_RISK_REDUCER_NAME,
  initialState: initialState(),
  reducers: { setCurrentPage, setSort, setNameFilter, setCiCdFilter, setSCMFilter, toggleFilterSideBar },
  extraReducers: {
    [loadAppIntegrationsAndRisk.pending]: loadAppIntegrationsAndRiskRequested,
    [loadAppIntegrationsAndRisk.fulfilled]: loadAppIntegrationsAndRiskFulfilled,
    [loadAppIntegrationsAndRisk.rejected]: loadAppIntegrationsAndRiskFailed,
  },
});

function initialState() {
  return {
    loading: false,
    loadError: null,
    tableData: [],
    pageCount: 0,
    pageSize: PAGE_SIZE,
    currentPage: 0,
    sort: `-${COLUMNS.EVALUATION}`,
    nameFilter: '',
    scmFilter: null,
    ciCdFilter: null,
    showFilterSideBar: false,
  };
}

export default appIntegrationsAndRiskSlice.reducer;

export const actions = {
  ...appIntegrationsAndRiskSlice.actions,
  loadAppIntegrationsAndRisk,
};
