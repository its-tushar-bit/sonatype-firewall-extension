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
  getWaiversConfigurationURL,
  getWaiversConfigurationURLnoStatus,
  getWaiversConfigurationURLWaiver,
} from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectSelectedOwnerTypeAndId, selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectWaivers } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';

const REDUCER_NAME = 'waiversConfiguration';

export const initialState = {
  loading: false,
  loadError: null,
  data: null,
  serverData: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
};

const loadWaiversConfigurationRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadWaiversConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadWaiversConfigurationFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadWaiversConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiversConfiguration`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    await dispatch(rootActions.loadSelectedOwner());
    return axios.get(getWaiversConfigurationURL(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadWaiversConfigurationPageRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadWaiversConfigurationPageFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
  state.serverData = payload;
};

const loadWaiversConfigurationPageFailed = (state, { payload }) => {
  state.data = null;
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadWaiversConfigurationPage = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiversConfiguration`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    try {
      const state = getState();
      let { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
      if (ownerId === undefined) {
        ({ ownerType, ownerId } = selectOwnerProperties(state));
      }
      await dispatch(rootActions.loadSelectedOwner());
      const response = await axios.get(getWaiversConfigurationURL(ownerType, ownerId));
      if (response.data.isInherited === true || response.data.isAutoWaiverEnabled === false) {
        return response.data;
      } else {
        try {
          const waiversId = response.data.autoPolicyWaiverId;
          const waiversData = await axios.get(getWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId));
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

const saveWaiversConfigurationRequested = (state) => {
  state.submitMaskState = false;
};

const saveWaiversConfigurationFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveWaiversConfigurationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveWaiversConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/saveWaiversConfiguration`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    let waiversId;
    try {
      waiversId = (await axios.get(getWaiversConfigurationURL(ownerType, ownerId))).data.autoPolicyWaiverId;
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
      .put(getWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadWaiversConfigurationPage())
        )
      )
      .catch(rejectWithValue);
  }
);

const createWaiverRequested = (state) => {
  state.submitMaskState = false;
};

const createWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const createWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const createWaiver = createAsyncThunk(
  `${REDUCER_NAME}/createWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    const putData = {
      threatLevel: 7,
      reachable: waivers.reachable,
      pathForward: waivers.pathForward,
    };
    return axios
      .post(getWaiversConfigurationURLnoStatus(ownerType, ownerId), putData)
      .then(prop('data'))
      .then(
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadWaiversConfigurationPage())
        )
      )
      .catch(rejectWithValue);
  }
);

const deleteWaiver = createAsyncThunk(
  `${REDUCER_NAME}/deleteWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaivers(state);
    const waiversId = waivers.autoPolicyWaiverId;
    return axios
      .delete(getWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId))
      .then(prop('data'))
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() =>
          dispatch(actions.loadWaiversConfigurationPage())
        );
      })
      .catch(rejectWithValue);
  }
);

const automatedWaiversSlice = createSlice({
  name: REDUCER_NAME,
  initialState,

  reducers: { toggleCheckboxReachable, toggleCheckboxPath, saveMaskTimerDone: propSet('submitMaskState', null) },
  extraReducers: {
    [createWaiver.pending]: createWaiverRequested,
    [createWaiver.fulfilled]: createWaiverFulfilled,
    [createWaiver.rejected]: createWaiverFailed,

    [saveWaiversConfiguration.pending]: saveWaiversConfigurationRequested,
    [saveWaiversConfiguration.fulfilled]: saveWaiversConfigurationFulfilled,
    [saveWaiversConfiguration.rejected]: saveWaiversConfigurationFailed,

    [loadWaiversConfiguration.pending]: loadWaiversConfigurationRequested,
    [loadWaiversConfiguration.fulfilled]: loadWaiversConfigurationFulfilled,
    [loadWaiversConfiguration.rejected]: loadWaiversConfigurationFailed,

    [loadWaiversConfigurationPage.pending]: loadWaiversConfigurationPageRequested,
    [loadWaiversConfigurationPage.fulfilled]: loadWaiversConfigurationPageFulfilled,
    [loadWaiversConfigurationPage.rejected]: loadWaiversConfigurationPageFailed,
  },
});

export const actions = {
  ...automatedWaiversSlice.actions,
  loadWaiversConfiguration,
  loadWaiversConfigurationPage,
  saveWaiversConfiguration,
  createWaiver,
  deleteWaiver,
};

export default automatedWaiversSlice.reducer;
