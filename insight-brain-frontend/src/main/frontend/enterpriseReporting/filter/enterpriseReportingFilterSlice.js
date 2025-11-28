/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { mapObjIndexed } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { toggleBooleanProp } from 'MainRoot/util/reduxUtil';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  selectEnterpriseReportingFilter,
  selectIsDefaultAlertRendered,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import {
  getEnterpriseReportingFilters,
  getDeleteEnterpriseReportingFilter,
  getDefaultEnterpriseReportingFilter,
  getAssignDefaultEnterpriseReportingFilter,
} from 'MainRoot/util/CLMLocation';
import { findFilterByName, findFilterById } from 'MainRoot/enterpriseReporting/utils';
import { WARNING_OVERWRITE, WARNING_NAME_IN_USE } from 'MainRoot/dashboard/filter/manageFiltersReducer';

const REDUCER_NAME = 'enterpriseReportingFilter';

export const FILTER_STATES = {
  CLEAN: 'clean',
  CHANGED: 'changed',
  APPLYING: 'applying',
};

export const initialState = {
  isOpen: false,
  loadingIframe: true,

  appliedFilterName: null, // saved filter name applied to Looker iframe (null when default)
  appliedFilter: null, // filter values applied to Looker iframe
  previewFilterName: null, // saved filter name to render in dropdown button
  previewFilter: null, // re-selected saved filter values to preview before applying to iframe
  defaultFilter: null, // Sonatype Default filter (determined by Looker)
  filterState: null,

  filtersInitialized: false, // Initial setup state - used for first-time loading and true once settled
  loadingAllFilters: true,

  loadingDefaultFilter: false,
  loadDefaultFilterError: null,
  saveDefaultFilterError: null,
  showDefaultFilterSuccessAlert: false,
  defaultFilterId: null, // user's selected default filter id

  loadingSavedFilters: false,
  loadSavedFiltersError: null,
  savedFilters: [],

  showUnsavedFilterModal: false,
  showSaveFilterModal: false,
  saveFilterMaskState: null,
  saveFilterWarning: null,
  saveFilterError: null,
  duplicateFilterName: null,

  filterToDelete: null,
  showDeleteFilterModal: false,
  deleteFilterMaskState: null,
  deleteFilterError: null,
};

export const EI_DEFAULT_FILTER_NAME = 'Sonatype Default';

const initializeFiltersRequested = (state) => {
  state.loadingAllFilters = true;
};

const initializeFiltersFulfilled = (state, { payload }) => {
  state.filtersInitialized = true;
  state.loadingAllFilters = false;
  state.appliedFilterName = payload;
  state.previewFilterName = payload;
};

const initializeFiltersFailed = (state) => {
  state.filtersInitialized = true;
  state.loadingAllFilters = false;
};

const initializeFilters = createAsyncThunk(`${REDUCER_NAME}/initializeFilters`, async (_, { dispatch, getState }) => {
  await Promise.all([dispatch(loadSavedFilters()), dispatch(loadDefaultFilter())]);

  const state = getState();
  const { appliedFilterName, defaultFilterId, savedFilters, filtersInitialized } = selectEnterpriseReportingFilter(
    state
  );

  // On first mount only, check if there's a custom default filter to apply
  if (!filtersInitialized && defaultFilterId) {
    const defaultFilter = findFilterById(defaultFilterId, savedFilters);
    return defaultFilter?.name ?? null;
  }

  // On subsequent calls or if no default filter, keep current appliedFilterName
  return appliedFilterName;
});

const loadSavedFiltersRequested = (state) => {
  state.loadingSavedFilters = true;
  state.loadSavedFiltersError = null;
};

const loadSavedFiltersFulfilled = (state, { payload }) => {
  state.loadingSavedFilters = false;
  state.savedFilters = payload;
};

const loadSavedFiltersFailed = (state, { payload }) => {
  state.loadingSavedFilters = false;
  state.loadSavedFiltersError = Messages.getHttpErrorMessage(payload);
};
const loadSavedFilters = createAsyncThunk(`${REDUCER_NAME}/loadSavedFilters`, async (_, { rejectWithValue }) => {
  try {
    const { data } = await axios.get(getEnterpriseReportingFilters());
    const parsedData = data.map((d) => {
      const filter = JSON.parse(d.filter);
      return { ...d, filter };
    });
    return parsedData;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const saveFilterRequested = (state) => {
  state.saveFilterMaskState = false;
  state.saveFilterError = null;
};

const saveFilterFulfilled = (state, { payload }) => {
  state.saveFilterMaskState = true;
  state.appliedFilterName = payload;
  state.previewFilterName = payload;
  state.saveFilterWarning = null;
  state.saveFilterError = null;
  state.filterState = FILTER_STATES.CLEAN;
};

const saveFilterFailed = (state, { payload }) => {
  state.saveFilterError = Messages.getHttpErrorMessage(payload);
  state.saveFilterMaskState = null;
};

const saveFilter = createAsyncThunk(
  `${REDUCER_NAME}/saveFilter`,
  async ({ name, isDefault, existingFilter }, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { appliedFilter } = selectEnterpriseReportingFilter(state);

    if (!appliedFilter || typeof appliedFilter !== 'object') {
      return rejectWithValue(new Error('Invalid filter data'));
    }
    const payload = { name, filter: JSON.stringify(appliedFilter), isDefault };
    try {
      const response = existingFilter
        ? await axios.put(getEnterpriseReportingFilters(), { id: existingFilter.id, ...payload })
        : await axios.post(getEnterpriseReportingFilters(), payload);

      dispatch(actions.loadSavedFilters());
      if (isDefault) {
        dispatch(actions.loadDefaultFilter());
      }
      setTimeout(() => {
        dispatch(actions.setShowSaveFilterModal(false));
      }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      return response.data.name;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const deleteFilterRequested = (state) => {
  state.deleteFilterMaskState = false;
};

const deleteFilterFulfilled = (state) => {
  state.deleteFilterMaskState = true;
  state.filterToDelete = null;
};

const deleteFilterFailed = (state, { payload }) => {
  state.deleteFilterError = Messages.getHttpErrorMessage(payload);
  state.deleteFilterMaskState = null;
};

const deleteFilter = createAsyncThunk(
  `${REDUCER_NAME}/deleteFilter`,
  (filterName, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const {
      appliedFilter,
      appliedFilterName,
      defaultFilter,
      defaultFilterId,
      previewFilterName,
      savedFilters,
    } = selectEnterpriseReportingFilter(state);
    const filterToDelete = findFilterByName(filterName, savedFilters);
    if (!filterToDelete) {
      return rejectWithValue(`Filter "${filterName}" not found. It may have already been deleted.`);
    }

    return axios
      .delete(getDeleteEnterpriseReportingFilter(filterToDelete.id))
      .then(() => {
        //If deleting filter that is currently applied, reset back to user's default filter (custom or Sonatype Default)
        if (appliedFilterName === filterName) {
          const userDefaultFilter =
            defaultFilterId && filterToDelete.id !== defaultFilterId
              ? findFilterById(defaultFilterId, savedFilters)
              : null;
          const filterNameToApply = userDefaultFilter?.name || null;
          const filterToApply = userDefaultFilter?.filter || defaultFilter;

          //set previewFilterName & previewFilter to be able to call applySavedFilterAndRunDashboard to update the iframe
          dispatch(actions.setPreviewFilterName(filterNameToApply));
          dispatch(actions.setPreviewFilter(filterToApply));
          dispatch(actions.applySavedFilterAndRunDashboard());
        } else if (previewFilterName === filterName) {
          //If deleting filter that is being previewed but not yet applied, reset to currently applied filted to match iframe
          dispatch(actions.setPreviewFilterName(appliedFilterName));
          dispatch(actions.setPreviewFilter(appliedFilter));
        }

        //If deleting the users's set default filter, dispatch load thunk again to refresh
        if (filterToDelete.id === defaultFilterId) {
          dispatch(actions.loadDefaultFilter());
        }

        dispatch(actions.loadSavedFilters());

        setTimeout(() => {
          dispatch(actions.setShowDeleteFilterModal(false));
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch(rejectWithValue);
  }
);

const loadDefaultFilterRequested = (state) => {
  state.loadingDefaultFilter = true;
  state.loadDefaultFilterError = null;
};

const loadDefaultFilterFulfilled = (state, { payload }) => {
  state.loadingDefaultFilter = false;
  state.defaultFilterId = payload;
};

const loadDefaultFilterFailed = (state, { payload }) => {
  state.loadingDefaultFilter = false;
  state.loadDefaultFilterError = Messages.getHttpErrorMessage(payload);
};

const loadDefaultFilter = createAsyncThunk(`${REDUCER_NAME}/loadDefaultFilter`, async (_, { rejectWithValue }) => {
  try {
    const { data } = await axios.get(getDefaultEnterpriseReportingFilter());
    return data === '' ? null : data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const saveDefaultFilterRequested = (state) => {
  state.saveDefaultFilterError = null;
  state.showDefaultFilterSuccessAlert = false;
};

const saveDefaultFilterFulfilled = (state, { payload }) => {
  state.defaultFilterId = payload;
  state.showDefaultFilterSuccessAlert = true;
};

const saveDefaultFilterFailed = (state, { payload }) => {
  state.saveDefaultFilterError = Messages.getHttpErrorMessage(payload);
};

const saveDefaultFilter = createAsyncThunk(
  `${REDUCER_NAME}/saveDefaultFilter`,
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState();
      const { appliedFilterName, savedFilters } = selectEnterpriseReportingFilter(state);
      const defaultFilter = findFilterByName(appliedFilterName, savedFilters);

      const { data } = await axios.put(getAssignDefaultEnterpriseReportingFilter(defaultFilter.id));
      return data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const deleteDefaultFilterRequested = (state) => {
  state.saveDefaultFilterError = null;
  state.showDefaultFilterSuccessAlert = false;
};

const deleteDefaultFilterFulfilled = (state) => {
  state.defaultFilterId = null;
  state.showDefaultFilterSuccessAlert = true;
};

const deleteDefaultFilterFailed = (state, { payload }) => {
  state.saveDefaultFilterError = Messages.getHttpErrorMessage(payload);
};

const deleteDefaultFilter = createAsyncThunk(`${REDUCER_NAME}/deleteDefaultFilter`, async (_, { rejectWithValue }) => {
  try {
    await axios.delete(getDefaultEnterpriseReportingFilter());
  } catch (error) {
    return rejectWithValue(error);
  }
});

const setDefaultFilter = (state, { payload }) => {
  state.defaultFilter = payload;
};

const setAppliedFilter = (state, { payload }) => {
  state.appliedFilter = payload;
};

const setPreviewFilter = (state, { payload }) => {
  state.previewFilter = payload;
};

const setFilterToDelete = (state, { payload }) => {
  state.filterToDelete = payload;
};

const setAppliedFilterName = (state, { payload }) => {
  state.appliedFilterName = payload;
};

const setPreviewFilterName = (state, { payload }) => {
  state.previewFilterName = payload;
};

const setDefaultFilterAsSelected = () => (dispatch, getState) => {
  const state = getState();
  const { defaultFilter } = selectEnterpriseReportingFilter(state);
  dispatch(actions.setSavedFilterAsSelected({ name: null, filter: defaultFilter }));
};

const setSavedFilterAsSelected = (filter) => (dispatch) => {
  dispatch(actions.setPreviewFilter(filter.filter));
  dispatch(actions.setPreviewFilterName(filter.name));
};

const applySavedFilterAndRunDashboard = () => (dispatch, getState) => {
  const state = getState();
  const { previewFilterName, previewFilter, defaultFilter } = selectEnterpriseReportingFilter(state);
  const isDefaultAlertRendered = selectIsDefaultAlertRendered(state);
  if (previewFilter) {
    // Merge with defaultFilter to clear any fields not in the saved filter
    const mergedFilter = { ...defaultFilter, ...previewFilter };

    dispatch(actions.setAppliedFilterName(previewFilterName));
    dispatch(actions.setAppliedFilter(mergedFilter));
    dispatch(actions.setFilterState(FILTER_STATES.APPLYING));

    if (isDefaultAlertRendered) {
      dispatch(actions.setClearDefaultAlert());
    }
    dispatch(actions.toggleShowFilter());
  }
};

const revertFilterChanges = () => (dispatch, getState) => {
  const state = getState();
  const { appliedFilterName, defaultFilter, savedFilters } = selectEnterpriseReportingFilter(state);
  const lastSavedFilter =
    appliedFilterName === null ? defaultFilter : findFilterByName(appliedFilterName, savedFilters)?.filter;

  // Merge with defaultFilter to clear any fields not in the saved filter
  const mergedFilter = { ...defaultFilter, ...lastSavedFilter };

  dispatch(actions.setAppliedFilter(mergedFilter));
  dispatch(actions.setFilterState(FILTER_STATES.APPLYING));
};

const trySaveFilter = ({ name, isDefault, isOverwriting }) => (dispatch, getState) => {
  const state = getState();
  const { savedFilters, saveFilterWarning } = selectEnterpriseReportingFilter(state);
  const existingFilter = findFilterByName(name, savedFilters);

  const duplicateName = existingFilter ? existingFilter.name : null;
  dispatch(actions.setDuplicateFilterName(duplicateName));

  if (existingFilter && !saveFilterWarning && !isOverwriting) {
    dispatch(actions.setSaveFilterWarning(WARNING_NAME_IN_USE));
  } else if (isOverwriting && !saveFilterWarning) {
    dispatch(actions.setSaveFilterWarning(WARNING_OVERWRITE));
  } else {
    // Warnings shown or no duplicate - proceed with save
    dispatch(actions.saveFilter({ name, isDefault, existingFilter }));
  }
};

const setLoadingIframe = (state, { payload }) => {
  state.loadingIframe = payload;
};

const setShowUnsavedFilterModal = (state, { payload }) => {
  state.showUnsavedFilterModal = payload;
};

const setShowSaveFilterModal = (state, { payload }) => {
  state.showSaveFilterModal = payload;
  state.saveFilterMaskState = null;
  state.saveFilterWarning = null;
  state.saveFilterError = null;
  state.duplicateFilterName = null;
};

const setShowDeleteFilterModal = (state, { payload }) => {
  state.showDeleteFilterModal = payload;
  state.deleteFilterMaskState = null;
  state.deleteFilterError = null;
};

const setSaveFilterWarning = (state, { payload }) => {
  state.saveFilterWarning = payload;
};

const setDuplicateFilterName = (state, { payload }) => {
  state.duplicateFilterName = payload;
};

const setClearDefaultAlert = (state) => {
  state.showDefaultFilterSuccessAlert = false;
  state.saveDefaultFilterError = null;
};

const setFilterState = (state, { payload }) => {
  state.filterState = payload;
};

// Persists appliedFilterName so Looker can apply the correct saved filter to the new dashboard.
// Also maintains filtersInitialized, which should remain true after first mount to prevent default
// filter from being applied to new dashboard on load instead of current appliedFilterName.
const reset = (state) => ({
  ...initialState,
  appliedFilterName: state.appliedFilterName,
  previewFilterName: state.appliedFilterName,
  filtersInitialized: state.filtersInitialized,
});

// Actions to be called during Looker javascript events:

// handles the iframe's "dashboard:loaded" event, which fires once on initial load
// Construct "Sonatype Default" filter by hardcoding default values for Stage & Date Range (matching Looker's
// defined default values), while clearing any other filters (which have no Looker defined default value).
const handleDashLoaded = (dashboardFilters) => (dispatch) => {
  const defaultedFilters = mapObjIndexed(
    (_, key) => (key === 'Stage' ? 'build' : key === 'Date Range' ? 'after 12 months ago' : ''),
    dashboardFilters
  );
  dispatch(actions.setDefaultFilter(defaultedFilters));
};

// Handles the iframe's "dashboard:filters:changed" event. NxDrawer doesn't close on click of iframe because
// doesn't have access to the iframe's event listeners. Close drawer when user makes a filter selection.
const handleDashChanged = () => (dispatch, getState) => {
  const state = getState();
  const { isOpen } = selectEnterpriseReportingFilter(state);
  if (isOpen) {
    dispatch(actions.toggleShowFilter());
    dispatch(actions.setClearDefaultAlert());
  }
};

// Handles the iframe's "dashboard:run:complete" event to sync redux filter states with iframe's current state
const handleDashUpdated = (dashboardFilters) => (dispatch, getState) => {
  const state = getState();
  const { filterState } = selectEnterpriseReportingFilter(state);

  dispatch(actions.setPreviewFilter(dashboardFilters));
  dispatch(actions.setAppliedFilter(dashboardFilters));

  // If 'applying' (reverted to or applied a save filter), or on initial load, filterState = clean
  // otherwise, user has manually adjusted filters in iframe, filterState = 'changed'
  const newFilterState = filterState === 'applying' || filterState === null ? 'clean' : 'changed';
  dispatch(actions.setFilterState(newFilterState));
  dispatch(actions.setLoadingIframe(false));
};

const enterpriseReportingFilterSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowFilter: toggleBooleanProp('isOpen'),
    setAppliedFilterName,
    setPreviewFilterName,
    setAppliedFilter,
    setPreviewFilter,
    setDefaultFilter,
    setFilterToDelete,
    setFilterState,
    setShowUnsavedFilterModal,
    setShowSaveFilterModal,
    setShowDeleteFilterModal,
    setSaveFilterWarning,
    setDuplicateFilterName,
    setClearDefaultAlert,
    setLoadingIframe,
    reset,
  },
  extraReducers: {
    [loadSavedFilters.pending]: loadSavedFiltersRequested,
    [loadSavedFilters.fulfilled]: loadSavedFiltersFulfilled,
    [loadSavedFilters.rejected]: loadSavedFiltersFailed,
    [saveFilter.pending]: saveFilterRequested,
    [saveFilter.fulfilled]: saveFilterFulfilled,
    [saveFilter.rejected]: saveFilterFailed,
    [deleteFilter.pending]: deleteFilterRequested,
    [deleteFilter.fulfilled]: deleteFilterFulfilled,
    [deleteFilter.rejected]: deleteFilterFailed,
    [loadDefaultFilter.pending]: loadDefaultFilterRequested,
    [loadDefaultFilter.fulfilled]: loadDefaultFilterFulfilled,
    [loadDefaultFilter.rejected]: loadDefaultFilterFailed,
    [initializeFilters.pending]: initializeFiltersRequested,
    [initializeFilters.fulfilled]: initializeFiltersFulfilled,
    [initializeFilters.rejected]: initializeFiltersFailed,
    [saveDefaultFilter.pending]: saveDefaultFilterRequested,
    [saveDefaultFilter.fulfilled]: saveDefaultFilterFulfilled,
    [saveDefaultFilter.rejected]: saveDefaultFilterFailed,
    [deleteDefaultFilter.pending]: deleteDefaultFilterRequested,
    [deleteDefaultFilter.fulfilled]: deleteDefaultFilterFulfilled,
    [deleteDefaultFilter.rejected]: deleteDefaultFilterFailed,
  },
});

export default enterpriseReportingFilterSlice.reducer;
export const actions = {
  ...enterpriseReportingFilterSlice.actions,
  loadSavedFilters,
  saveFilter,
  deleteFilter,
  loadDefaultFilter,
  initializeFilters,
  saveDefaultFilter,
  deleteDefaultFilter,
  applySavedFilterAndRunDashboard,
  revertFilterChanges,
  trySaveFilter,
  setSavedFilterAsSelected,
  setDefaultFilterAsSelected,
  handleDashLoaded,
  handleDashChanged,
  handleDashUpdated,
};
