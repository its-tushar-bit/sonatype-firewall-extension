/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { debounce } from 'debounce';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  getUserActivityUrl,
  getUserActivityExportUrl,
  getUserActivityFilterOptionsUrl,
} from 'MainRoot/util/CLMLocation';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { formatDateAsYYYYMMDD } from 'MainRoot/util/dateUtils';

const REDUCER_NAME = 'userActivity';

// Search debounce configuration
const SEARCH_DEBOUNCE_TIME = 500;
const MIN_SEARCH_CHARACTERS = 3;

// Pagination configuration
export const USER_ACTIVITY_PAGE_SIZE = 25;

// Helper function to calculate offset for pagination
export const calculateOffset = (page) => page * USER_ACTIVITY_PAGE_SIZE;

// Helper function to check if filters are dirty (like Dashboard)
function areFiltersDirty(selectedFilters, appliedFilters) {
  return JSON.stringify(selectedFilters) !== JSON.stringify(appliedFilters);
}

// Helper function to convert Age filter to date ranges
export function calculateDateRange(ageInDays) {
  // Get today's date in YYYY-MM-DD format.
  const today = new Date();
  const endUtcDate = formatDateAsYYYYMMDD(today);

  // Calculate start date by creating a new date and subtracting days
  const startDateObj = new Date(today);
  startDateObj.setDate(startDateObj.getDate() - ageInDays);
  const startUtcDate = formatDateAsYYYYMMDD(startDateObj);

  return {
    startUtcDate,
    endUtcDate,
  };
}

const defaultFilters = {
  selectedAge: 30, // Default to 'past 30 days' like dashboard
};

export const initialState = {
  users: [],
  totalUsers: 0,
  dateRange: {
    startDate: null,
    endDate: null,
  },
  pagination: {
    limit: 100,
    offset: 0,
    hasMore: false,
  },
  loading: false,
  loadError: null,
  exporting: false,
  exportError: null,
  // Search state
  searchFilter: '',
  // Filter drawer state
  filterDrawerOpen: false,
  // Two-state filter pattern like Dashboard
  selectedFilters: { ...defaultFilters }, // Current UI selections
  appliedFilters: { ...defaultFilters }, // Filters applied to data
  filtersAreDirty: false, // Comparison between selected vs applied
  // Details page state
  detailsCurrentUser: null,
  detailsActivities: [],
  detailsTotalActivities: 0,
  detailsLoading: false,
  detailsLoadError: null,
  detailsExporting: false,
  detailsExportError: null,
  detailsPagination: { limit: USER_ACTIVITY_PAGE_SIZE, offset: 0, hasMore: false },
  // Details filter state
  detailsFilterDrawerOpen: false,
  detailsSelectedFilters: {
    selectedActivityTypes: [],
    selectedDomains: [],
    selectedErrorTypes: [],
  },
  detailsAppliedFilters: {
    selectedActivityTypes: [],
    selectedDomains: [],
    selectedErrorTypes: [],
  },
  detailsFiltersAreDirty: false,
  // Filter options
  filterOptions: {
    activityTypes: [],
    domains: [],
    errorTypes: [],
  },
  filterOptionsLoading: false,
  filterOptionsError: null,
};

export const loadUserActivity = createAsyncThunk(
  `${REDUCER_NAME}/loadUserActivity`,
  async (params, { rejectWithValue }) => {
    try {
      const queryParams = new URLSearchParams();

      if (params.startUtcDate) queryParams.append('startUtcDate', params.startUtcDate);
      if (params.endUtcDate) queryParams.append('endUtcDate', params.endUtcDate);
      if (params.username) queryParams.append('username', params.username);
      if (params.limit) queryParams.append('limit', params.limit);
      if (params.offset) queryParams.append('offset', params.offset);

      const response = await axios.get(`${getUserActivityUrl()}?${queryParams.toString()}`);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const applyFilters = createAsyncThunk(`${REDUCER_NAME}/applyFilters`, async (params, { getState, dispatch }) => {
  const state = getState();
  const { selectedFilters } = state.userActivity;

  const dateRange = calculateDateRange(selectedFilters.selectedAge);

  const apiParams = {
    ...dateRange,
    username: params?.username || null,
    limit: params?.limit || USER_ACTIVITY_PAGE_SIZE,
    offset: params?.offset || 0,
  };

  return dispatch(loadUserActivity(apiParams));
});

export const exportUserActivityData = createAsyncThunk(
  `${REDUCER_NAME}/exportUserActivityData`,
  async (params, { rejectWithValue }) => {
    let url, link;
    try {
      const queryParams = new URLSearchParams();

      if (params.startUtcDate) queryParams.append('startUtcDate', params.startUtcDate);
      if (params.endUtcDate) queryParams.append('endUtcDate', params.endUtcDate);
      if (params.username) queryParams.append('username', params.username);

      // Handle multi-select arrays for export
      if (params.activityTypes && Array.isArray(params.activityTypes)) {
        params.activityTypes.forEach((type) => queryParams.append('activityTypes', type));
      }

      if (params.domains && Array.isArray(params.domains)) {
        params.domains.forEach((domain) => queryParams.append('domains', domain));
      }

      if (params.errorTypes && Array.isArray(params.errorTypes)) {
        params.errorTypes.forEach((type) => queryParams.append('errorTypes', type));
      }

      const response = await axios.get(`${getUserActivityExportUrl()}?${queryParams.toString()}`, {
        responseType: 'blob',
        headers: {
          Accept: 'text/csv, application/csv',
        },
      });

      const timestamp = new Date().toISOString().slice(0, 19).replace(/[:-]/g, '');
      const filename = params.username
        ? `user_activity_detail_${params.username}_${timestamp}.csv`
        : `user_activity_all_${timestamp}.csv`;

      url = window.URL.createObjectURL(new Blob([response.data]));
      link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();

      return { filename, size: response.data.size };
    } catch (error) {
      return rejectWithValue(error);
    } finally {
      if (url) {
        window.URL.revokeObjectURL(url);
      }
      if (link) {
        link.remove();
      }
    }
  }
);

// Load user activity page with permission check (consistent with Users tab)
export const loadUserActivityPage = createAsyncThunk(
  `${REDUCER_NAME}/loadUserActivityPage`,
  async (params = {}, { dispatch, rejectWithValue }) => {
    try {
      // Check both CONFIGURE_SYSTEM and ACCESS_AUDIT_LOG permissions for user activity
      await checkPermissions(['CONFIGURE_SYSTEM', 'ACCESS_AUDIT_LOG']);

      // Apply default filters and load data
      return dispatch(
        applyFilters({
          username: params.username || null,
          limit: params.limit || USER_ACTIVITY_PAGE_SIZE,
          offset: params.offset || 0,
        })
      );
    } catch (error) {
      return rejectWithValue(Messages.getHttpErrorMessage(error));
    }
  }
);

// Debounced search implementation (like Reports page)
const searchUsersDebounce = debounce((dispatch, getState) => {
  const state = getState();
  const { searchFilter, selectedFilters } = state.userActivity;

  // Only trigger API call if search has 3+ characters or is empty (to show all)
  if (searchFilter.length === 0 || searchFilter.length >= MIN_SEARCH_CHARACTERS) {
    const dateRange = calculateDateRange(selectedFilters.selectedAge);

    const apiParams = {
      ...dateRange,
      username: searchFilter || null,
      limit: USER_ACTIVITY_PAGE_SIZE,
      offset: 0,
    };

    dispatch(loadUserActivity(apiParams));
  }
}, SEARCH_DEBOUNCE_TIME);

export const searchUsers = (searchValue) => (dispatch, getState) => {
  // Immediately update search filter in state (no API call)
  dispatch(userActivitySlice.actions.setSearchFilter(searchValue));

  // Trigger debounced API call
  searchUsersDebounce(dispatch, getState);
};

export const loadUserActivityDetail = createAsyncThunk(
  `${REDUCER_NAME}/loadUserActivityDetail`,
  async (params, { rejectWithValue }) => {
    try {
      const queryParams = new URLSearchParams();

      if (params.startUtcDate) queryParams.append('startUtcDate', params.startUtcDate);
      if (params.endUtcDate) queryParams.append('endUtcDate', params.endUtcDate);
      if (params.limit) queryParams.append('limit', params.limit);
      if (params.offset) queryParams.append('offset', params.offset);

      // Handle multi-select arrays
      if (params.activityTypes && Array.isArray(params.activityTypes)) {
        params.activityTypes.forEach((type) => queryParams.append('activityTypes', type));
      }

      if (params.domains && Array.isArray(params.domains)) {
        params.domains.forEach((domain) => queryParams.append('domains', domain));
      }

      if (params.errorTypes && Array.isArray(params.errorTypes)) {
        params.errorTypes.forEach((type) => queryParams.append('errorTypes', type));
      }

      const fullUrl = `${getUserActivityUrl()}/${params.username}?${queryParams.toString()}`;

      const response = await axios.get(fullUrl);
      return response.data;
    } catch (error) {
      console.error('loadUserActivityDetail - API error:', error);
      return rejectWithValue(error);
    }
  }
);

export const loadFilterOptions = createAsyncThunk(
  `${REDUCER_NAME}/loadFilterOptions`,
  async (params, { rejectWithValue }) => {
    try {
      const response = await axios.get(getUserActivityFilterOptionsUrl());
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const applyDetailsFilters = createAsyncThunk(
  `${REDUCER_NAME}/applyDetailsFilters`,
  async (params, { getState, dispatch }) => {
    const state = getState();
    const { detailsSelectedFilters, appliedFilters } = state.userActivity;

    const dateRange = calculateDateRange(appliedFilters.selectedAge);

    const apiParams = {
      ...dateRange,
      username: params.username,
      limit: params?.limit || USER_ACTIVITY_PAGE_SIZE,
      offset: params?.offset || 0,
      activityTypes:
        detailsSelectedFilters.selectedActivityTypes?.length > 0
          ? detailsSelectedFilters.selectedActivityTypes
          : undefined,
      domains: detailsSelectedFilters.selectedDomains?.length > 0 ? detailsSelectedFilters.selectedDomains : undefined,
      errorTypes:
        detailsSelectedFilters.selectedErrorTypes?.length > 0 ? detailsSelectedFilters.selectedErrorTypes : undefined,
    };

    return dispatch(loadUserActivityDetail(apiParams));
  }
);

const userActivitySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    clearErrors: (state) => {
      state.loadError = null;
      state.exportError = null;
      state.detailsExportError = null;
    },
    setSearchFilter: (state, action) => {
      state.searchFilter = action.payload;
    },
    toggleFilterDrawer: (state, action) => {
      state.filterDrawerOpen = action.payload !== undefined ? action.payload : !state.filterDrawerOpen;
    },
    setSelectedAge: (state, action) => {
      // Update selectedFilters only (like Dashboard)
      state.selectedFilters.selectedAge = action.payload;
      // Calculate if filters are dirty
      state.filtersAreDirty = areFiltersDirty(state.selectedFilters, state.appliedFilters);
    },
    revertFilters: (state) => {
      // Copy applied back to selected (like Dashboard revertFilter)
      state.selectedFilters = { ...state.appliedFilters };
      state.filtersAreDirty = false;
    },
    // Details page actions
    setDetailsCurrentUser: (state, action) => {
      state.detailsCurrentUser = action.payload;
    },
    clearDetailsData: (state) => {
      state.detailsCurrentUser = null;
      state.detailsActivities = [];
      state.detailsTotalActivities = 0;
      state.detailsLoadError = null;
    },
    toggleDetailsFilterDrawer: (state, action) => {
      state.detailsFilterDrawerOpen = action.payload !== undefined ? action.payload : !state.detailsFilterDrawerOpen;
    },
    setSelectedActivityTypes: (state, action) => {
      state.detailsSelectedFilters.selectedActivityTypes = action.payload || [];
      state.detailsFiltersAreDirty = areFiltersDirty(state.detailsSelectedFilters, state.detailsAppliedFilters);
    },
    setSelectedDomains: (state, action) => {
      state.detailsSelectedFilters.selectedDomains = action.payload || [];
      state.detailsFiltersAreDirty = areFiltersDirty(state.detailsSelectedFilters, state.detailsAppliedFilters);
    },
    setSelectedErrorTypes: (state, action) => {
      state.detailsSelectedFilters.selectedErrorTypes = action.payload || [];
      state.detailsFiltersAreDirty = areFiltersDirty(state.detailsSelectedFilters, state.detailsAppliedFilters);
    },
    revertDetailsFilters: (state) => {
      state.detailsSelectedFilters = { ...state.detailsAppliedFilters };
      state.detailsFiltersAreDirty = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadUserActivity.pending, (state) => {
        state.loading = true;
        state.loadError = null;
      })
      .addCase(loadUserActivity.fulfilled, (state, action) => {
        const payload = action.payload;
        state.loading = false;
        state.loadError = null;
        state.users = payload.users || [];
        state.totalUsers = payload.totalUsers || 0;

        if (payload.dateRange) {
          state.dateRange = payload.dateRange;
        }

        if (payload.pagination) {
          state.pagination = payload.pagination;
        }
      })
      .addCase(loadUserActivity.rejected, (state, action) => {
        state.loading = false;
        state.loadError = Messages.getHttpErrorMessage(action.payload);
      })
      .addCase(applyFilters.fulfilled, (state) => {
        // Copy selected to applied (like Dashboard)
        state.appliedFilters = { ...state.selectedFilters };
        state.filtersAreDirty = false;
      })
      .addCase(loadUserActivityPage.pending, (state) => {
        state.loading = true;
        state.loadError = null;
      })
      .addCase(loadUserActivityPage.fulfilled, (state) => {
        // Copy selected to applied (like Dashboard) when permission check passes
        state.appliedFilters = { ...state.selectedFilters };
        state.filtersAreDirty = false;
        // Loading state handled by nested applyFilters action
      })
      .addCase(loadUserActivityPage.rejected, (state, action) => {
        state.loading = false;
        state.loadError = Messages.getHttpErrorMessage(action.payload);
      })
      .addCase(exportUserActivityData.pending, (state) => {
        state.exporting = true;
        state.exportError = null;
      })
      .addCase(exportUserActivityData.fulfilled, (state) => {
        state.exporting = false;
        state.exportError = null;
      })
      .addCase(exportUserActivityData.rejected, (state, action) => {
        state.exporting = false;
        state.exportError = Messages.getHttpErrorMessage(action.payload);
      })
      .addCase(loadUserActivityDetail.pending, (state) => {
        state.detailsLoading = true;
        state.detailsLoadError = null;
      })
      .addCase(loadUserActivityDetail.fulfilled, (state, action) => {
        const payload = action.payload;
        state.detailsLoading = false;
        state.detailsLoadError = null;
        state.detailsActivities = payload.activities || [];
        state.detailsTotalActivities = payload.totalActivities || payload.activities?.length || 0;

        // Store pagination information for cursor-based pagination
        if (payload.pagination) {
          state.detailsPagination = payload.pagination;
        }

        if (payload.username) {
          state.detailsCurrentUser = payload.username;
        }
      })
      .addCase(loadUserActivityDetail.rejected, (state, action) => {
        state.detailsLoading = false;
        state.detailsLoadError = Messages.getHttpErrorMessage(action.payload);
      })
      .addCase(loadFilterOptions.pending, (state) => {
        state.filterOptionsLoading = true;
        state.filterOptionsError = null;
      })
      .addCase(loadFilterOptions.fulfilled, (state, action) => {
        const payload = action.payload;
        state.filterOptionsLoading = false;
        state.filterOptionsError = null;
        state.filterOptions = {
          activityTypes: payload.activityTypes || [],
          domains: payload.domains || [],
          errorTypes: payload.errorTypes || [],
        };
      })
      .addCase(loadFilterOptions.rejected, (state, action) => {
        state.filterOptionsLoading = false;
        state.filterOptionsError = Messages.getHttpErrorMessage(action.payload);
      })
      .addCase(applyDetailsFilters.fulfilled, (state) => {
        // Copy selected to applied filters (like Dashboard pattern)
        state.detailsAppliedFilters = { ...state.detailsSelectedFilters };
        state.detailsFiltersAreDirty = false;
      });
  },
});

export const {
  clearErrors,
  toggleFilterDrawer,
  setSelectedAge,
  revertFilters,
  setSearchFilter,
  // Details page actions
  setDetailsCurrentUser,
  clearDetailsData,
  toggleDetailsFilterDrawer,
  setSelectedActivityTypes,
  setSelectedDomains,
  setSelectedErrorTypes,
  revertDetailsFilters,
} = userActivitySlice.actions;
export const { actions } = userActivitySlice;
export default userActivitySlice.reducer;
