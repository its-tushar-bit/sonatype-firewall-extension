/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { getReact2ShellReportDataUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'react2Shell';
export const PAGE_SIZE = 50;
export const REACT2SHELL_CVE_IDS = ['CVE-2025-55182', 'CVE-2025-55183', 'CVE-2025-55184', 'CVE-2025-67779'];

export const initialState = {
  loading: false,
  sorting: false, // re-loading via a sort change
  error: null,
  aggregates: null,
  impactData: null,
  pagination: null,
  currentPage: 0,
  sortBy: null,
  sortOrder: 'asc',
};

const loadRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadFulfilled = (state, { payload }) => {
  state.loading = false;

  state.aggregates = {
    affectedApplications: payload.affectedApplications,
    affectedComponents: payload.affectedComponents,
    violatingComponents: payload.violatingComponents,
    activeWaivers: payload.activeWaivers,
  };

  state.impactData = payload.impactData;
  state.pagination = payload.pagination;
  state.error = null;
};

const loadFailed = (state, { payload }) => {
  state.loading = false;
  state.error = payload;
};

const transformApiResponse = (apiResponse) => {
  const { aggregates, results, pageNumber, pageSize, totalCount } = apiResponse;

  return {
    affectedApplications: aggregates?.totalAffectedApplications || 0,
    affectedComponents: aggregates?.affectedComponents || 0,
    violatingComponents: aggregates?.violatingComponents || 0,
    activeWaivers: aggregates?.activeWaivers || 0,
    pagination: {
      page: pageNumber,
      pageSize: pageSize,
      totalItems: totalCount,
      totalPages: Math.ceil(totalCount / pageSize),
    },
    impactData: (results || []).map((item) => ({
      ...item,
      evaluation: item.activeWaiver ? 'Waived' : item.violating ? 'Fail' : 'Pass',
      version: item.packageUrl?.split('@')[1]?.split('?')[0] || '',
    })),
  };
};

export const fetchReportData = createAsyncThunk(
  `${REDUCER_NAME}/fetchReportData`,
  async ({ pageNumber = 1, pageSize = PAGE_SIZE, sortBy = null, sortOrder = 'asc' }, { rejectWithValue }) => {
    try {
      const url = getReact2ShellReportDataUrl(REACT2SHELL_CVE_IDS, pageNumber, pageSize, sortBy, sortOrder);
      const response = await axios.get(url);
      const apiData = response.data;

      return transformApiResponse(apiData);
    } catch (error) {
      return rejectWithValue(Messages.getHttpErrorMessage(error));
    }
  }
);

const setPage = (state, action) => {
  state.currentPage = action.payload;
};

const setSort = (state, action) => {
  const { column } = action.payload;
  if (state.sortBy === column) {
    state.sortOrder = state.sortOrder === 'asc' ? 'desc' : 'asc';
  } else {
    state.sortBy = column;
    state.sortOrder = 'asc';
  }
  state.currentPage = 0;
};

export const fetchWithSort = createAsyncThunk(
  `${REDUCER_NAME}/fetchWithSort`,
  async ({ column }, { getState, rejectWithValue }) => {
    try {
      const state = getState()[REDUCER_NAME];

      // Determine new sort order based on current sort state
      let newSortBy = column;
      let newSortOrder = 'asc';

      if (state.sortBy === column) {
        newSortOrder = state.sortOrder === 'asc' ? 'desc' : 'asc';
      }

      const url = getReact2ShellReportDataUrl(REACT2SHELL_CVE_IDS, 1, PAGE_SIZE, newSortBy, newSortOrder);
      const response = await axios.get(url);
      const apiData = response.data;

      return {
        ...transformApiResponse(apiData),
        sortBy: newSortBy,
        sortOrder: newSortOrder,
      };
    } catch (error) {
      return rejectWithValue(Messages.getHttpErrorMessage(error));
    }
  }
);

const fetchWithSortRequested = (state) => {
  state.sorting = true;
  state.error = null;
};

const fetchWithSortFulfilled = (state, { payload }) => {
  state.sorting = false;
  state.aggregates = {
    affectedApplications: payload.affectedApplications,
    affectedComponents: payload.affectedComponents,
    violatingComponents: payload.violatingComponents,
    activeWaivers: payload.activeWaivers,
  };
  state.impactData = payload.impactData;
  state.pagination = payload.pagination;
  state.sortBy = payload.sortBy;
  state.sortOrder = payload.sortOrder;
  state.currentPage = 0;
  state.error = null;
};

const fetchWithSortFailed = (state, { payload }) => {
  state.sorting = false;
  state.error = payload;
};

const react2ShellSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: () => initialState,
    setPage,
    setSort,
  },
  extraReducers: {
    [fetchReportData.pending]: loadRequested,
    [fetchReportData.fulfilled]: loadFulfilled,
    [fetchReportData.rejected]: loadFailed,
    [fetchWithSort.pending]: fetchWithSortRequested,
    [fetchWithSort.fulfilled]: fetchWithSortFulfilled,
    [fetchWithSort.rejected]: fetchWithSortFailed,
  },
});

export const actions = {
  ...react2ShellSlice.actions,
  fetchReportData,
  fetchWithSort,
};

export default react2ShellSlice.reducer;
