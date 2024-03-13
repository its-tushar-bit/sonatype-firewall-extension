/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { debounce } from 'debounce';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getRepositoryComponentNamePatternUpdateUrl, getRepositoryComponentNameUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectComponentNamePatterns,
  selectComponentsRequestBody,
  selectCurrentPage,
} from './namespaceConfusionProtectionTileSelectors';
import { changeMultiColumnSortCriteria } from 'MainRoot/util/jsUtil';
import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

const REDUCER_NAME = 'namespaceConfusionProtectionTile';
const PAGES = 1;
const PAGE_SIZE = 6;
const FILTER_DEBOUNCE_TIME = 500;
const DEFAULT_SORTED_FIELD_PROPRIETARY = 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME';
export const DEFAULT_KEY_FILTER_SECTION = 'repository_managers';

const initialNamePatternsTableConfig = {
  page: PAGES,
  pageSize: PAGE_SIZE,
  searchFilters: [],
  sortFields: [
    {
      columnName: DEFAULT_SORTED_FIELD_PROPRIETARY,
      dir: 'asc',
    },
  ],
};

const initialState = {
  componentNamePatterns: {
    [DEFAULT_KEY_FILTER_SECTION]: [],
  },
  hasNextPage: {
    [DEFAULT_KEY_FILTER_SECTION]: null,
  },
  loadingComponentNamePatterns: false,
  errorComponentsTable: null,
  namePatternsTableConfig: {
    [DEFAULT_KEY_FILTER_SECTION]: initialNamePatternsTableConfig,
  },
  searchFiltersValues: {
    [DEFAULT_KEY_FILTER_SECTION]: {
      [DEFAULT_SORTED_FIELD_PROPRIETARY]: '',
    },
  },
  currentFilterKey: DEFAULT_KEY_FILTER_SECTION,
  updatingComponentNamePattern: false,
  errorUpdatingComponentNamePattern: null,
};

const sortComponents = (sortData) => (dispatch) => {
  dispatch(actions.setSorting(sortData));
  dispatch(actions.getComponentNamePatterns());
};

const setSorting = (state, { payload }) => {
  state.namePatternsTableConfig[state.currentFilterKey].sortFields = changeMultiColumnSortCriteria(
    payload,
    state.namePatternsTableConfig[state.currentFilterKey].sortFields
  );
};

const getComponentNamePatterns = createAsyncThunk(
  `${REDUCER_NAME}/getComponentNamePatterns`,
  (_, { getState, rejectWithValue }) => {
    const namePatternsTableConfig = selectComponentsRequestBody(getState());
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios
      .post(getRepositoryComponentNameUrl(ownerType, ownerId), namePatternsTableConfig)
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const applyFilters = (state) => {
  state.namePatternsTableConfig.page = 1;
  state.namePatternsTableConfig.matchStateFilters = [...state.selectedMatchStateFilters];
  state.namePatternsTableConfig.violationStateFilters = [...state.selectedViolationStateFilters];
};

const getComponentNamePatternsPending = (state) => {
  state.loadingComponentNamePatterns = true;
  state.errorComponentsTable = null;
};
const getComponentNamePatternsFulfilled = (state, { payload }) => {
  state.loadingComponentNamePatterns = false;
  state.errorComponentsTable = null;
  state.componentNamePatterns[state.currentFilterKey] = payload.proprietaryComponentNamePatterns;
  state.hasNextPage[state.currentFilterKey] = payload.hasNextPage;
};

const getComponentNamePatternsRejected = (state, { payload }) => {
  state.loadingComponentNamePatterns = false;
  state.errorComponentsTable = Messages.getHttpErrorMessage(payload);
};

const increasePage = (state) => {
  state.namePatternsTableConfig[state.currentFilterKey].page += 1;
};

const decreasePage = (state) => {
  state.namePatternsTableConfig[state.currentFilterKey].page -= 1;
};

const loadNextPage = () => (dispatch) => {
  dispatch(actions.increasePage());
  dispatch(actions.getComponentNamePatterns());
};

const loadPreviousPage = () => (dispatch, getState) => {
  const currentPage = selectCurrentPage(getState());
  if (currentPage > 1) {
    dispatch(actions.decreasePage());
    dispatch(actions.getComponentNamePatterns());
  }
};

const setFilter = (state, { payload: { filterName, filterValue } }) => {
  let searchFilter = state.namePatternsTableConfig[state.currentFilterKey].searchFilters.find(
    (filter) => filter.filterableField === filterName
  );

  if (searchFilter) {
    searchFilter.value = filterValue;
  } else {
    state.namePatternsTableConfig[state.currentFilterKey].searchFilters.push({
      filterableField: filterName,
      value: filterValue,
    });
  }

  state.searchFiltersValues[state.currentFilterKey] = state.searchFiltersValues[state.currentFilterKey] ?? {};
  state.searchFiltersValues[state.currentFilterKey][filterName] = filterValue;

  state.namePatternsTableConfig[state.currentFilterKey].searchFilters = state.namePatternsTableConfig[
    state.currentFilterKey
  ].searchFilters.filter((searchFilter) => searchFilter.value !== '');
};

const setCurrentFilterKey = (state, { payload }) => {
  state.currentFilterKey = payload;

  if (!state.namePatternsTableConfig[payload]) {
    state.namePatternsTableConfig[payload] = initialNamePatternsTableConfig;
  }

  if (!state.componentNamePatterns[payload]) {
    state.componentNamePatterns[payload] = [];
  }

  if (!state.searchFiltersValues[payload]) {
    state.searchFiltersValues[payload] = {
      [DEFAULT_SORTED_FIELD_PROPRIETARY]: '',
    };
  }

  if (!state.hasNextPage[payload]) {
    state.hasNextPage[payload] = null;
  }
};

const filterComponentsDebounce = debounce((dispatch) => {
  dispatch(actions.getComponentNamePatterns());
}, FILTER_DEBOUNCE_TIME);

const searchComponents = (searchData) => (dispatch) => {
  dispatch(actions.setFilter(searchData));
  filterComponentsDebounce(dispatch);
};

const updateComponentNamePatternPending = (state) => {
  state.updatingComponentNamePattern = true;
  state.errorUpdatingComponentNamePattern = null;
};

const updateComponentNamePatternFulfilled = (state) => {
  state.updatingComponentNamePattern = false;
  state.errorUpdatingComponentNamePattern = null;
};

const updateComponentNamePatternRejected = (state, { payload }) => {
  state.updatingComponentNamePattern = false;
  state.errorUpdatingComponentNamePattern = Messages.getHttpErrorMessage(payload);
};

const setEnabledStatus = (selectedComponentId) => (dispatch) => {
  dispatch(actions.updateComponentNamePattern(selectedComponentId));
};

const updateComponentNamePattern = createAsyncThunk(
  `${REDUCER_NAME}/updateComponentNamePattern`,
  (selectedComponentId, { getState, dispatch, rejectWithValue }) => {
    const components = selectComponentNamePatterns(getState());
    const component = components.find((component) => component.id === selectedComponentId);
    return axios
      .post(getRepositoryComponentNamePatternUpdateUrl(), { ...component, enabled: !component.enabled })
      .then(() => dispatch(actions.getComponentNamePatterns()))
      .catch(rejectWithValue);
  }
);

export const namespaceConfusionProtectionTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSorting,
    setFilter,
    applyFilters,
    decreasePage,
    increasePage,
    setCurrentFilterKey,
  },
  extraReducers: {
    [getComponentNamePatterns.pending]: getComponentNamePatternsPending,
    [getComponentNamePatterns.fulfilled]: getComponentNamePatternsFulfilled,
    [getComponentNamePatterns.rejected]: getComponentNamePatternsRejected,
    [updateComponentNamePattern.pending]: updateComponentNamePatternPending,
    [updateComponentNamePattern.rejected]: updateComponentNamePatternRejected,
    [updateComponentNamePattern.fulfilled]: updateComponentNamePatternFulfilled,
  },
});

export const actions = {
  ...namespaceConfusionProtectionTileSlice.actions,
  getComponentNamePatterns,
  sortComponents,
  searchComponents,
  loadPreviousPage,
  loadNextPage,
  setEnabledStatus,
  updateComponentNamePattern,
};
export default namespaceConfusionProtectionTileSlice.reducer;
