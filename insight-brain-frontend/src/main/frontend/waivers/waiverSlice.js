/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { nxDateInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getPolicyWaiverReasonsUrl,
  getBulkWaiverUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { waiverMatcherStrategy, isCustomExpiryTimeSelected } from 'MainRoot/util/waiverUtils';
import { getFutureDate } from 'MainRoot/util/jsUtil';
import { equals, prop } from 'ramda';
import moment from 'moment';
import { selectBulkWaiverSelectedViolations, selectBulkWaiverConfiguration } from './bulkWaiverSelectors';

const REDUCER_NAME = 'waivers';

const initialState = {
  waiverReasons: {
    loading: false,
    loadError: null,
    data: [],
  },
  bulkWaive: {
    checkboxState: {},
    selectAllChecked: false,
    selectedViolations: [],
    waiverConfiguration: {
      waiverReasonId: '',
      expiryTime: '',
      customExpiryTime: nxDateInputStateHelpers.initialState(''),
      comments: '',
      componentMatcherStrategy: '',
      selectedWaiverScope: null,
    },
    submitMaskState: null,
    submitError: null,
  },
  permissions: {
    loading: {},
    error: {},
    byApplicationId: {},
  },
};

const loadCachedWaiverReasons = createAsyncThunk(
  `${REDUCER_NAME}/loadCachedWaiverReasons`,
  (_, { getState, rejectWithValue }) => {
    const waiverReasons = getState().waivers.waiverReasons.data;
    const waiverReasonsPromise =
      waiverReasons.length > 0 ? Promise.resolve({ data: waiverReasons }) : axios.get(getPolicyWaiverReasonsUrl());
    return waiverReasonsPromise.then(prop('data')).catch(rejectWithValue);
  }
);

const loadPermissionForAppWaivers = createAsyncThunk(
  `${REDUCER_NAME}/loadPermissionForAppWaivers`,
  async (applicationPublicId, { getState, rejectWithValue }) => {
    // Check if permission is already cached
    const cachedPermission = getState().waivers.permissions.byApplicationId[applicationPublicId];
    if (cachedPermission !== undefined) {
      return { applicationPublicId, hasPermission: cachedPermission };
    }

    try {
      const { data: appSummary } = await axios.get(getApplicationSummaryUrl(applicationPublicId));
      const { data: permissions } = await axios.put(getPermissionContextTestUrl('application', appSummary.id), [
        'WAIVE_POLICY_VIOLATIONS',
      ]);
      return { applicationPublicId, hasPermission: permissions.length === 1 };
    } catch (error) {
      return rejectWithValue({ applicationPublicId, error });
    }
  }
);

const addBulkWaiver = createAsyncThunk(
  `${REDUCER_NAME}/addBulkWaiver`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const selectedViolations = selectBulkWaiverSelectedViolations(state);
    const waiverConfiguration = selectBulkWaiverConfiguration(state);
    const ownerType = waiverConfiguration?.selectedWaiverScope?.type;
    const ownerId = waiverConfiguration?.selectedWaiverScope?.id;

    // Helper function to check if a violation is unknown
    const isUnknownComponent = (v) => !v.matchState || v.matchState === 'unknown';

    // Get the component matcher strategy
    const componentMatcherStrategy = waiverConfiguration.componentMatcherStrategy || waiverMatcherStrategy.ALL_VERSIONS;

    // If "All Versions" is selected, filter out unknown components
    const violationsToSubmit =
      componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS
        ? selectedViolations.filter((v) => !isUnknownComponent(v))
        : selectedViolations;

    // Extract violation IDs from filtered list
    const violationIds = violationsToSubmit.map((v) => v.policyViolationId);

    // Transform configuration to API format
    const apiWaiverOptionsDTO = {
      comment: waiverConfiguration.comments || null,
      matcherStrategy: waiverConfiguration.componentMatcherStrategy || waiverMatcherStrategy.ALL_VERSIONS,
      expiryTime:
        waiverConfiguration.expiryTime === null
          ? null
          : isCustomExpiryTimeSelected(waiverConfiguration.expiryTime)
          ? moment(waiverConfiguration.customExpiryTime.value).endOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ')
          : getFutureDate(waiverConfiguration.expiryTime),
      waiverReasonId: waiverConfiguration.waiverReasonId,
    };

    const requestBody = {
      violationIds,
      apiWaiverOptionsDTO,
    };

    return axios
      .post(getBulkWaiverUrl(ownerType, ownerId), requestBody)
      .then((response) => {
        startSubmitMaskSuccessTimer(dispatch);
        return response.data;
      })
      .catch(rejectWithValue);
  }
);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.submitMaskTimerDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const loadCachedWaiverReasonsRequested = (state) => {
  state.waiverReasons.loading = true;
  state.waiverReasons.loadError = null;
};

const loadCachedWaiverReasonsFulfilled = (state, { payload }) => {
  state.waiverReasons.loading = false;
  state.waiverReasons.loadError = null;
  state.waiverReasons.data = payload;
};

const loadCachedWaiverReasonsFailed = (state, { payload }) => {
  state.waiverReasons.loading = false;
  state.waiverReasons.loadError = Messages.getHttpErrorMessage(payload);
};

const loadPermissionForAppWaiversRequested = (state, { meta }) => {
  const applicationPublicId = meta.arg;
  state.permissions.loading[applicationPublicId] = true;
  delete state.permissions.error[applicationPublicId];
};

const loadPermissionForAppWaiversFulfilled = (state, { payload }) => {
  const { applicationPublicId, hasPermission } = payload;
  state.permissions.loading[applicationPublicId] = false;
  state.permissions.byApplicationId[applicationPublicId] = hasPermission;
  delete state.permissions.error[applicationPublicId];
};

const loadPermissionForAppWaiversFailed = (state, { payload }) => {
  const { applicationPublicId, error } = payload;
  state.permissions.loading[applicationPublicId] = false;
  state.permissions.byApplicationId[applicationPublicId] = false;
  state.permissions.error[applicationPublicId] = Messages.getHttpErrorMessage(error);
};

const addBulkWaiverRequested = (state) => {
  state.bulkWaive.submitMaskState = false;
  state.bulkWaive.submitError = null;
};

const addBulkWaiverFulfilled = (state) => {
  state.bulkWaive.submitMaskState = true;
  state.bulkWaive.submitError = null;
};

const addBulkWaiverFailed = (state, { payload }) => {
  state.bulkWaive.submitMaskState = null;
  state.bulkWaive.submitError = Messages.getHttpErrorMessage(payload);
};

const toggleBulkWaiveCheckbox = (state, action) => {
  const id = action.payload;
  state.bulkWaive.checkboxState[id] = !state.bulkWaive.checkboxState[id];
};

const clearBulkWaiveCheckboxes = (state) => {
  state.bulkWaive.checkboxState = {};
  state.bulkWaive.selectAllChecked = false;
};

const setSelectedViolations = (state, action) => {
  // Check if the selected violations have changed
  if (!equals(state.bulkWaive.selectedViolations, action.payload)) {
    state.bulkWaive.selectedViolations = action.payload;

    // Reset waiver configuration ONLY if it has been modified from initial state
    // This handles the case where user configured a waiver, then goes back and changes selections
    // We don't want to reset while they're initially building their selection
    const isConfigurationModified = !equals(
      state.bulkWaive.waiverConfiguration,
      initialState.bulkWaive.waiverConfiguration
    );

    if (isConfigurationModified) {
      // Create a fresh copy to avoid shared references with initialState
      state.bulkWaive.waiverConfiguration = {
        ...initialState.bulkWaive.waiverConfiguration,
      };
    }
  }
};

const toggleSelectAllCheckbox = (state, action) => {
  const { ids, shouldSelect = true } = action.payload;

  if (shouldSelect) {
    // Select all provided ids
    ids.forEach((id) => {
      state.bulkWaive.checkboxState[id] = true;
    });
  } else {
    // Only uncheck the provided ids, keep others
    ids.forEach((id) => {
      delete state.bulkWaive.checkboxState[id];
    });
  }
};

const setWaiverConfiguration = (state, action) => {
  state.bulkWaive.waiverConfiguration = action.payload;
};

const resetBulkWaiverSubmitState = (state) => {
  state.bulkWaive.submitMaskState = null;
  state.bulkWaive.submitError = null;
};

const resetWaiverConfiguration = (state) => {
  state.bulkWaive.waiverConfiguration = initialState.bulkWaive.waiverConfiguration;
};

const submitMaskTimerDone = (state) => {
  state.bulkWaive.submitMaskState = null;
};

const waiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleBulkWaiveCheckbox,
    clearBulkWaiveCheckboxes,
    setSelectedViolations,
    toggleSelectAllCheckbox,
    setWaiverConfiguration,
    resetBulkWaiverSubmitState,
    submitMaskTimerDone,
    resetWaiverConfiguration,
  },
  extraReducers: {
    [loadCachedWaiverReasons.pending]: loadCachedWaiverReasonsRequested,
    [loadCachedWaiverReasons.fulfilled]: loadCachedWaiverReasonsFulfilled,
    [loadCachedWaiverReasons.rejected]: loadCachedWaiverReasonsFailed,
    [loadPermissionForAppWaivers.pending]: loadPermissionForAppWaiversRequested,
    [loadPermissionForAppWaivers.fulfilled]: loadPermissionForAppWaiversFulfilled,
    [loadPermissionForAppWaivers.rejected]: loadPermissionForAppWaiversFailed,
    [addBulkWaiver.pending]: addBulkWaiverRequested,
    [addBulkWaiver.fulfilled]: addBulkWaiverFulfilled,
    [addBulkWaiver.rejected]: addBulkWaiverFailed,
  },
});

export default waiverSlice.reducer;
export const actions = {
  ...waiverSlice.actions,
  loadCachedWaiverReasons,
  loadPermissionForAppWaivers,
  addBulkWaiver,
};
