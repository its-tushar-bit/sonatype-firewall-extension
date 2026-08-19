/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { equals } from 'ramda';
import { selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import axios from 'axios';
import {
  getAutoWaiversConfigurationURL,
  getAutoWaiversConfigurationURLnoStatus,
  getAutoWaiversConfigurationURLWaiver,
} from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { selectWaiver } from './autoWaiverModalSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import { propSet } from 'MainRoot/util/jsUtil';
import { actions as applicableAutoWaiversActions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/applicableAutoWaiversSlice';

const REDUCER_NAME = 'autoWaiverActions/autoWaiverModal';
const DEFAULT_CONFIG = {
  reachability: false,
  pathForward: false,
  threatLevel: 7,
  scope: 'any',
};

export const initialState = Object.freeze({
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
  isEditMode: false,
  isDirty: false,
  isUnsavedChangesModalOpen: false,
  data: DEFAULT_CONFIG,
  serverData: DEFAULT_CONFIG,
});

const setIsDirty = (state, { payload }) => {
  return { ...state, isDirty: payload };
};

const computeIsDirty = (state) => {
  const { data, serverData } = state;
  const isDirty = !equals(data, serverData);
  return { ...state, isDirty };
};

const closeModal = (state, { payload }) => {
  if (payload?.isDirty) {
    state.isUnsavedChangesModalOpen = true;
    return;
  }

  state.submitError = null;
  state.isModalOpen = false;
  state.isEditMode = false;
  state.isDirty = false;
  state.isUnsavedChangesModalOpen = false;
  state.data = DEFAULT_CONFIG;
  state.serverData = DEFAULT_CONFIG;
};

const openModal = (state) => {
  state.submitMaskState = null;
  state.isModalOpen = true;
};

const openEditModal = (state, { payload }) => {
  state.submitMaskState = null;
  state.isModalOpen = true;
  state.isEditMode = true;
  state.data = payload;
  state.serverData = payload;
};

const closeUnsavedChangesModal = (state) => {
  state.isUnsavedChangesModalOpen = false;
};

const toggleCheckboxNoUpgradePath = (state) => {
  const newData = {
    ...state.data,
    pathForward: !state.data?.pathForward,
  };
  return computeIsDirty({ ...state, data: newData });
};

const toggleCheckboxReachability = (state) => {
  const newData = {
    ...state.data,
    reachability: !state.data?.reachability,
  };
  return computeIsDirty({ ...state, data: newData });
};

const setThreatLevel = (state, { payload }) => {
  const newData = {
    ...state.data,
    threatLevel: payload,
  };
  return computeIsDirty({ ...state, data: newData });
};

const setScope = (state, { payload }) => {
  const newData = {
    ...state.data,
    scope: payload,
  };
  return computeIsDirty({ ...state, data: newData });
};

const createAutoWaiverRequested = (state) => {
  state.submitMaskState = false;
};

const createAutoWaiverFulfilled = (state) => {
  state.isDirty = false;
  state.submitError = null;
  state.submitMaskState = true;
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
    const waivers = selectWaiver(state);
    const putData = {
      reachability: waivers.reachability,
      pathForward: waivers.pathForward,
      threatLevel: waivers.threatLevel,
      scopesOperatorAny: waivers.scope !== 'all',
    };
    return axios
      .post(getAutoWaiversConfigurationURLnoStatus(ownerType, ownerId), putData)
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => {
          dispatch(applicableAutoWaiversActions.loadApplicableAutoWaivers());
          dispatch(actions.closeModal());
        });
      })
      .catch(rejectWithValue);
  }
);

const saveAutoWaiverRequested = (state) => {
  state.submitMaskState = false;
};

const saveAutoWaiverFulfilled = (state) => {
  state.submitMaskState = true;
  state.submitError = null;
  state.isDirty = false;
};

const saveAutoWaiverFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveAutoWaiver = createAsyncThunk(
  `${REDUCER_NAME}/saveAutoWaiver`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectSelectedOwnerTypeAndId(state);
    const waivers = selectWaiver(state);
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
      reachability: waivers.reachability,
      pathForward: waivers.pathForward,
      scopesOperatorAny: waivers.scope !== 'all',
    };
    return axios
      .put(getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiversId), putData)
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.closeModal);
      })
      .catch(rejectWithValue);
  }
);

const autoWaiver = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setIsDirty,
    openModal,
    openEditModal,
    closeModal,
    closeUnsavedChangesModal,
    toggleCheckboxNoUpgradePath,
    toggleCheckboxReachability,
    setThreatLevel,
    setScope,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [createAutoWaiver.pending]: createAutoWaiverRequested,
    [createAutoWaiver.fulfilled]: createAutoWaiverFulfilled,
    [createAutoWaiver.rejected]: createAutoWaiverFailed,

    [saveAutoWaiver.pending]: saveAutoWaiverRequested,
    [saveAutoWaiver.fulfilled]: saveAutoWaiverFulfilled,
    [saveAutoWaiver.rejected]: saveAutoWaiverFailed,
  },
});

export default autoWaiver.reducer;

export const actions = {
  ...autoWaiver.actions,
  createAutoWaiver,
  saveAutoWaiver,
};
