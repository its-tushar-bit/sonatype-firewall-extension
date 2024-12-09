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
import {
  getAutoWaiversConfigurationURL,
  getAutoWaiversConfigurationURLnoStatus,
  getAutoWaiversConfigurationURLWaiver,
} from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectSelectedOwnerTypeAndId, selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectWaivers } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';

const REDUCER_NAME = 'autoWaiversConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
};

const loadAutoWaiversConfigurationRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAutoWaiversConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadAutoWaiversConfigurationFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadAutoWaiversConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/loadAutoWaiversConfiguration`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    await dispatch(rootActions.loadSelectedOwner());
    return axios.get(getAutoWaiversConfigurationURL(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadAutoWaiversConfigurationPageRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadAutoWaiversConfigurationPageFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadAutoWaiversConfigurationPageFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadAutoWaiversConfigurationPage = createAsyncThunk(
  `${REDUCER_NAME}/loadAutoWaiversConfiguration`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    try {
      const state = getState();
      let { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
      if (ownerId === undefined) {
        ({ ownerType, ownerId } = selectOwnerProperties(state));
      }
      await dispatch(rootActions.loadSelectedOwner());
      const response = await axios.get(getAutoWaiversConfigurationURL(ownerType, ownerId));
      if (response.data.isInherited === true || response.data.isAutoWaiverEnabled === false) {
        return response.data;
      } else {
        try {
          const waiversId = response.data.autoPolicyWaiverId;
          const waiversData = await axios.get(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId));
          return waiversData.data;
        } catch (error) {
          return rejectWithValue(error.response ? error.response.data : error.message);
        }
      }
    } catch (error) {
      return rejectWithValue(error.response ? error.response.data : error.message);
    }
  }
);

const toggleCheckboxReachable = (state) => {
  const newData = {
    ...state.data,
    reachable: !(state.data?.reachable ?? false),
  };
  return computeIsDirty({ ...state, data: newData });
};

const toggleCheckboxPath = (state) => {
  const newData = {
    ...state.data,
    pathForward: !(state.data?.pathForward ?? false),
  };
  return computeIsDirty({ ...state, data: newData });
};

const computeIsDirty = (state) => {
  const { data, serverData } = state;
  const isDirty = !equals(data, serverData);
  return { ...state, isDirty };
};

const saveAutoWaiversConfigurationRequested = (state) => {
  state.submitMaskState = false;
};

const saveAutoWaiversConfigurationFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveAutoWaiversConfigurationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveAutoWaiversConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/saveAutoWaiversConfiguration`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    let waiversId;
    try {
      waiversId = (await axios.get(getAutoWaiversConfigurationURL(ownerType, ownerId))).data.autoPolicyWaiverId;
    } catch (error) {
      return rejectWithValue(error.response ? error.response.data : error.message);
    }
    const putData = {
      threatLevel: waivers.threatLevel,
      autoPolicyWaiverId: waivers.autoPolicyWaiverId,
      ownerId: waivers.ownerId,
      reachable: waivers.reachable,
      pathForward: waivers.pathForward,
    };
    return axios
      .put(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadAutoWaiversConfigurationPage())
        )
      )
      .catch(rejectWithValue);
  }
);

const createAutoWaiverRequested = (state) => {
  state.submitMaskState = false;
};

const createAutoWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const createAutoWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const createAutoWaiver = createAsyncThunk(
  `${REDUCER_NAME}/createAutoWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    const putData = {
      threatLevel: waivers.threatLevel,
      reachable: waivers.reachable,
      pathForward: waivers.pathForward,
    };
    return axios
      .post(getAutoWaiversConfigurationURLnoStatus(ownerType, ownerId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadAutoWaiversConfigurationPage())
        )
      )
      .catch(rejectWithValue);
  }
);

const deleteAutoWaiver = createAsyncThunk(
  `${REDUCER_NAME}/deleteWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    const waiversId = waivers.autoPolicyWaiverId;
    return axios
      .delete(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId))
      .then(prop('data'))
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadWaiversConfigurationPage())
        );
      })
      .catch(rejectWithValue);
  }
);

function setThreatLevel(state, { payload }) {
  const newData = {
    ...state.data,
    threatLevel: payload,
  };
  return computeIsDirty({ ...state, data: newData });
}

const automatedWaiversSlice = createSlice({
  name: REDUCER_NAME,
  initialState,

  reducers: {
    toggleCheckboxReachable,
    toggleCheckboxPath,
    setThreatLevel,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [createAutoWaiver.pending]: createAutoWaiverRequested,
    [createAutoWaiver.fulfilled]: createAutoWaiverFulfilled,
    [createAutoWaiver.rejected]: createAutoWaiverFailed,

    [saveAutoWaiversConfiguration.pending]: saveAutoWaiversConfigurationRequested,
    [saveAutoWaiversConfiguration.fulfilled]: saveAutoWaiversConfigurationFulfilled,
    [saveAutoWaiversConfiguration.rejected]: saveAutoWaiversConfigurationFailed,

    [loadAutoWaiversConfiguration.pending]: loadAutoWaiversConfigurationRequested,
    [loadAutoWaiversConfiguration.fulfilled]: loadAutoWaiversConfigurationFulfilled,
    [loadAutoWaiversConfiguration.rejected]: loadAutoWaiversConfigurationFailed,

    [loadAutoWaiversConfigurationPage.pending]: loadAutoWaiversConfigurationPageRequested,
    [loadAutoWaiversConfigurationPage.fulfilled]: loadAutoWaiversConfigurationPageFulfilled,
    [loadAutoWaiversConfigurationPage.rejected]: loadAutoWaiversConfigurationPageFailed,
  },
});

export const actions = {
  ...automatedWaiversSlice.actions,
  loadAutoWaiversConfiguration,
  loadAutoWaiversConfigurationPage,
  saveAutoWaiversConfiguration,
  createAutoWaiver,
  deleteAutoWaiver,
};

export default automatedWaiversSlice.reducer;
