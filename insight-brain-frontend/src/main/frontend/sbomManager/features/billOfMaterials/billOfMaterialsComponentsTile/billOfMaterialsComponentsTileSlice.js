/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  always,
  assoc,
  compose,
  cond,
  equals,
  findIndex,
  has,
  includes,
  keys,
  pickBy,
  T,
  values,
  when,
  without,
} from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getBillOfMaterialsComponentsUrl } from 'MainRoot/util/CLMLocation';

import { selectBillOfMaterialsComponentsTile } from './billOfMaterialsComponentsTileSelectors';

const REDUCER_NAME = 'billOfMaterialsComponentsTile';

export const COMPONENTS_PER_PAGE = 50;

export const SORT_BY_FIELDS = Object.freeze({
  type: 'type',
  vulnerabilities: 'vulnerabilities',
  releaseStatusPercentage: 'release_status_percentage',
  displayName: 'display_name',
});

export const SORT_DIRECTION = Object.freeze({
  ASC: 'asc',
  DESC: 'desc',
  DEFAULT: null,
});

export const defaultSortConfiguration = Object.freeze({
  sortBy: SORT_BY_FIELDS.vulnerabilities,
  sortDirection: SORT_DIRECTION.DESC,
});

export const defaultFilterConfiguration = Object.freeze({
  vulnerabilityThreatLevels: {
    critical: false,
    high: false,
    medium: false,
    low: false,
  },
  dependencyTypes: {
    direct: false,
    transitive: false,
    unspecified: false,
  },
});

const filterDrawerInitialState = Object.freeze({
  showDrawer: false,
  collapsibleItems: {
    showVulnerabilityThreatLevels: true,
    showDependencyTypes: true,
  },
});

export const paginationInitialState = Object.freeze({
  pageCount: 1,
  currentPage: 0,
});

export const initialState = Object.freeze({
  loadingComponents: true,
  loadingComponentsErrorMessage: null,

  components: null,
  totalNumberOfComponents: null,

  sortConfiguration: { ...defaultSortConfiguration },
  filterConfiguration: { ...defaultFilterConfiguration },
  pagination: { ...paginationInitialState },

  filterDrawer: { ...filterDrawerInitialState },

  componentSearch: null,
});

// load-components
const resetLoadComponentsConfigurations = (state) => {
  state.sortConfiguration = { ...defaultSortConfiguration };
  state.filterConfiguration = { ...defaultFilterConfiguration };
  state.pagination = { ...paginationInitialState };
};

const setLoadingComponents = (state, { payload }) => {
  state.loadingComponents = payload;
};

const loadComponentsRequested = (state) => {
  state.loadingComponents = true;
  state.loadingComponentsErrorMessage = null;

  state.components = null;
  state.totalNumberOfComponents = null;
};

const loadComponentsFailed = (state, { payload }) => {
  state.loadingComponents = false;
  state.loadingComponentsErrorMessage = Messages.getHttpErrorMessage(payload);

  state.components = null;
  state.totalNumberOfComponents = null;

  state.pagination = { ...paginationInitialState };
};

const loadComponentsFulfilled = (state, { payload }) => {
  state.loadingComponents = false;
  state.loadingComponentsErrorMessage = null;

  state.components = payload.results;
  state.totalNumberOfComponents = payload.totalResultsCount;

  state.pagination.pageCount = Math.ceil(payload.totalResultsCount / COMPONENTS_PER_PAGE);
};

const loadComponents = createAsyncThunk(
  `${REDUCER_NAME}/loadComponents`,
  async ({ internalAppId, sbomVersion }, { getState, rejectWithValue }) => {
    const state = getState();
    const { sortConfiguration, filterConfiguration, pagination, componentSearch } = selectBillOfMaterialsComponentsTile(
      state
    );

    const pickKeysWithTrueValue = compose(
      keys,
      pickBy((v) => !!v)
    );

    const sortDirection = cond([
      [equals(SORT_DIRECTION.ASC), always(true)],
      [equals(SORT_DIRECTION.DESC), always(false)],
      [T, always(null)],
    ])(sortConfiguration.sortDirection);

    return axios
      .get(
        getBillOfMaterialsComponentsUrl(
          internalAppId,
          sbomVersion,
          pagination.currentPage + 1,
          COMPONENTS_PER_PAGE,
          sortConfiguration.sortBy,
          sortDirection,
          pickKeysWithTrueValue(filterConfiguration.vulnerabilityThreatLevels),
          pickKeysWithTrueValue(filterConfiguration.dependencyTypes),
          componentSearch
        )
      )
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

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

// filter-configuration
const setFilterVulnerabilityThreatLevels = (state, { payload: { field, value } }) => {
  state.filterConfiguration.vulnerabilityThreatLevels = when(
    has(field),
    assoc(field, value)
  )(state.filterConfiguration.vulnerabilityThreatLevels);
};

const setFilterDependencyTypes = (state, { payload: { field, value } }) => {
  state.filterConfiguration.dependencyTypes = when(
    has(field),
    assoc(field, value)
  )(state.filterConfiguration.dependencyTypes);
};

// filter-drawer
const toggleShowFilterDrawer = (state) => {
  state.filterDrawer.showDrawer = !state.filterDrawer.showDrawer;
};

const setShowVulnerabilityThreatLevelsCollapsibleItems = (state, { payload }) => {
  state.filterDrawer.collapsibleItems.showVulnerabilityThreatLevels = payload;
};

const setShowDependencyTypesCollapsibleItems = (state, { payload }) => {
  state.filterDrawer.collapsibleItems.showDependencyTypes = payload;
};

// pagination
const setCurrentPage = (state, { payload }) => {
  state.pagination.currentPage = payload;
};

// component-name-search
const setComponentSearch = (state, { payload }) => {
  state.componentSearch = payload;
};

const billOfMaterialsComponentsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetLoadComponentsConfigurations,
    setLoadingComponents,
    setSortByAndCycleDirection,
    toggleShowFilterDrawer,
    setShowVulnerabilityThreatLevelsCollapsibleItems,
    setShowDependencyTypesCollapsibleItems,
    setFilterVulnerabilityThreatLevels,
    setFilterDependencyTypes,
    setCurrentPage,
    setComponentSearch,
  },
  extraReducers: {
    [loadComponents.pending]: loadComponentsRequested,
    [loadComponents.fulfilled]: loadComponentsFulfilled,
    [loadComponents.rejected]: loadComponentsFailed,
  },
});

export const actions = {
  ...billOfMaterialsComponentsTileSlice.actions,
  loadComponents,
};

export default billOfMaterialsComponentsTileSlice.reducer;
