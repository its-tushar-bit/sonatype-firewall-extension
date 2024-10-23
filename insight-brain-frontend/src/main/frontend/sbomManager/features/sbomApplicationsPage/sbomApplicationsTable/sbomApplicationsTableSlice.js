/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
// import axios from 'axios';
import { findIndex, includes, values, without } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';

// https://sonatype.atlassian.net/browse/SBOM-891
// import { getSbomApplicationsUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'sbomApplicationsTable';

export const APPLICATIONS_PER_PAGE = 50;

export const SORT_BY_FIELDS = Object.freeze({
  name: 'name',
  latestVersion: 'latest_version',
  importDate: 'import_date',
  vulnerabilities: 'vulnerabilities',
  violations: 'violations',
  annotated: 'annotated',
});

export const SORT_DIRECTION = Object.freeze({
  ASC: 'asc',
  DESC: 'desc',
  DEFAULT: null,
});

export const defaultSortConfiguration = Object.freeze({
  sortBy: SORT_BY_FIELDS.importDate,
  sortDirection: SORT_DIRECTION.ASC,
});

export const paginationInitialState = Object.freeze({
  pageCount: 1,
  currentPage: 0,
});

export const initialState = Object.freeze({
  loading: true,
  errorMessage: null,

  applications: null,
  applicationsTotalCount: null,

  sortConfiguration: { ...defaultSortConfiguration },
  pagination: { ...paginationInitialState },

  filterApplicationName: null,
});

// load-components
const resetConfigurations = (state) => {
  state.sortConfiguration = { ...defaultSortConfiguration };
  state.pagination = { ...paginationInitialState };
};

const setLoading = (state, { payload }) => {
  state.loading = payload;
};

const loadApplicationsRequested = (state) => {
  state.loading = true;
  state.errorMessage = null;
  state.applications = null;
  state.applicationsTotalCount = null;
};

const loadApplicationsFailed = (state, { payload }) => {
  state.loading = false;
  state.errorMessage = Messages.getHttpErrorMessage(payload);

  state.applications = null;
  state.applicationsTotalCount = null;

  state.pagination = { ...paginationInitialState };
};

const loadApplicationsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.errorMessage = null;

  state.applications = payload.results;
  state.applicationsTotalCount = payload.totalResultsCount;

  state.pagination.pageCount = Math.ceil(payload.totalResultsCount / APPLICATIONS_PER_PAGE);
};

const mockApplications = Object.freeze([
  {
    applicationPublicId: '123',
    name: 'very-very-very-very-long-application-name',
    latestVersion: 'very-very-very-very-very-long-sbom-version',
    importDate: '2024-10-16T17:25:01Z',
    criticalCount: 1,
    severeCount: 2,
    moderateCount: 3,
    lowCount: 4,
    annotated: 50,
  },
]);

const loadApplications = createAsyncThunk(`${REDUCER_NAME}/loadApplications`, async () => {
  return Promise.resolve({ results: mockApplications, totalResultsCount: 1 });
  // TODO: To be completed once the API is ready.
  // const state = getState();
  // const {
  //   sortConfiguration,
  //   filterConfiguration,
  //   pagination,
  //   filterApplicationName,
  // } = selectSbomApplicationsTable(state);

  // const pickKeysWithTrueValue = compose(
  //   keys,
  //   pickBy((v) => !!v)
  // );

  // const sortDirection = cond([
  //   [equals(SORT_DIRECTION.ASC), always(true)],
  //   [equals(SORT_DIRECTION.DESC), always(false)],
  //   [T, always(null)],
  // ])(sortConfiguration.sortDirection);

  // return axios
  //   .get(
  //     getSbomApplicationsUrl()
  //   )
  //   .then((response) => response.data)
  //   .catch((err) => rejectWithValue(err));
});

// sort-configuration
const setSortByAndCycleDirection = (state, { payload: newSortBy }) => {
  const cycleList = (list, current) => {
    const index = findIndex((item) => item === current, list);
    return list[(index + 1) % list.length];
  };

  const currentSortBy = state.sortConfiguration.sortBy;
  const currentDirection = state.sortConfiguration.sortDirection;
  const { sortBy: defaultSortBy, sortDirection: defaultSortDirection } = defaultSortConfiguration;
  const sortDirections = values(SORT_DIRECTION);

  if (newSortBy === defaultSortBy) {
    const complement = [defaultSortDirection, SORT_DIRECTION.DEFAULT];
    if (newSortBy !== currentSortBy) {
      state.sortConfiguration.sortDirection = sortDirections[0];
    } else if (includes(currentDirection, complement)) {
      state.sortConfiguration.sortDirection = cycleList(without(complement, sortDirections), currentDirection);
    } else if (includes(cycleList(sortDirections, currentDirection), complement)) {
      state.sortConfiguration.sortDirection = defaultSortDirection;
    }
    state.sortConfiguration.sortBy = newSortBy;
  } else {
    const nextDirection = newSortBy !== currentSortBy ? sortDirections[0] : cycleList(sortDirections, currentDirection);
    state.sortConfiguration =
      nextDirection === SORT_DIRECTION.DEFAULT
        ? { ...defaultSortConfiguration }
        : { sortBy: newSortBy, sortDirection: nextDirection };
  }
};

// pagination
const setCurrentPage = (state, { payload }) => {
  state.pagination.currentPage = payload;
};

// component-name-search
const setFilterApplicationName = (state, { payload }) => {
  state.filterApplicationName = payload;
};

const sbomApplicationsTableSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetConfigurations,
    setLoading,
    setSortByAndCycleDirection,
    setCurrentPage,
    setFilterApplicationName,
  },
  extraReducers: {
    [loadApplications.pending]: loadApplicationsRequested,
    [loadApplications.fulfilled]: loadApplicationsFulfilled,
    [loadApplications.rejected]: loadApplicationsFailed,
  },
});

export const actions = {
  ...sbomApplicationsTableSlice.actions,
  loadApplications,
};

export default sbomApplicationsTableSlice.reducer;
