/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { equals, prop } from 'ramda';
import { propSet } from 'MainRoot/util/jsUtil';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { getLegacyViolationURL } from 'MainRoot/util/CLMLocation';
import { selectLegacyViolation } from './legacyViolationSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

const REDUCER_NAME = 'legacyViolation';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
};

const loadLegacyViolationRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadLegacyViolationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadLegacyViolationFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadLegacyViolation = createAsyncThunk(
  `${REDUCER_NAME}/loadLegacyViolation`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    await dispatch(rootActions.loadSelectedOwner());
    return axios.get(getLegacyViolationURL(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const saveLegacyViolationRequested = (state) => {
  state.submitMaskState = false;
};

const saveLegacyViolationFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveLegacyViolationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveLegacyViolation = createAsyncThunk(
  `${REDUCER_NAME}/saveLegacyViolation`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const data = selectLegacyViolation(state);

    const putData = {
      allowOverride: data.allowOverride,
      enabled: data.inheritedFromOrganizationName ? null : data.enabled,
    };

    return axios
      .put(getLegacyViolationURL(ownerType, ownerId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadLegacyViolation())
        )
      )
      .catch(rejectWithValue);
  }
);

const setLegacyViolationStatus = (state, { payload }) => {
  let newData;
  switch (payload) {
    case 'inherit':
      newData = {
        ...state.data,
        enabled: state.serverData.enabled ? true : false,
        inheritedFromOrganizationName: state.serverData.inheritedFromOrganizationName
          ? state.serverData.inheritedFromOrganizationName
          : 'inherit',
      };
      break;
    case 'enabled':
      newData = { ...state.data, enabled: true, inheritedFromOrganizationName: null };
      break;
    case 'disabled':
      newData = { ...state.data, enabled: false, inheritedFromOrganizationName: null };
      break;
  }
  return computeIsDirty({ ...state, data: newData });
};

const toggleOverride = (state) => {
  const newData = { ...state.data, allowOverride: !state.data.allowOverride };
  return computeIsDirty({ ...state, data: newData });
};

const computeIsDirty = (state) => {
  const { data, serverData } = state;
  const isDirty = !equals(data, serverData);
  return { ...state, isDirty };
};

const legacyViolationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,

  reducers: { setLegacyViolationStatus, toggleOverride, saveMaskTimerDone: propSet('submitMaskState', null) },
  extraReducers: {
    [loadLegacyViolation.pending]: loadLegacyViolationRequested,
    [loadLegacyViolation.fulfilled]: loadLegacyViolationFulfilled,
    [loadLegacyViolation.rejected]: loadLegacyViolationFailed,

    [saveLegacyViolation.pending]: saveLegacyViolationRequested,
    [saveLegacyViolation.fulfilled]: saveLegacyViolationFulfilled,
    [saveLegacyViolation.rejected]: saveLegacyViolationFailed,
  },
});

export const actions = {
  ...legacyViolationSlice.actions,
  loadLegacyViolation,
  saveLegacyViolation,
};

export default legacyViolationSlice.reducer;
