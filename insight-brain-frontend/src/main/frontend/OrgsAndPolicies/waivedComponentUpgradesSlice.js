/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { propSet } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getWaivedComponentUpgradeConfigUrl } from 'MainRoot/util/CLMLocation';
import { selectCliStagesWithNoneOption } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { selectConfiguredStage } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSelectors';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const REDUCER_NAME = 'waivedComponentUpgrades';

export const initialState = {
  loading: false,
  loadError: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
  configuredStage: null,
};

// get stage for update notification
const loadUpgradeStage = createAsyncThunk(
  `${REDUCER_NAME}/loadUpgradeStage`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const stages = selectCliStagesWithNoneOption(getState());
    const promises = [axios.get(getWaivedComponentUpgradeConfigUrl())];

    if (!stages) {
      promises.push(dispatch(stagesActions.loadCliStages()));
    }

    return Promise.all(promises)
      .then(([{ data }]) => data)
      .catch(rejectWithValue);
  }
);

const getUpgradeStageRequested = (state) => ({
  ...state,
  loading: true,
  loadError: null,
});

const getUpgradeStageFailed = (state, { payload }) => ({
  ...state,
  loading: false,
  loadError: Messages.getHttpErrorMessage(payload),
  configuredStage: null,
});

const getUpgradeStageFulfilled = (state, { payload }) => ({
  ...state,
  loading: false,
  configuredStage: payload.stage,
});

// Save stage for upgrade indicators
const saveUpgradeStage = createAsyncThunk(
  `${REDUCER_NAME}/saveUpgradeStage`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const stageToSave = {
      stage: selectConfiguredStage(getState()),
    };

    return axios
      .put(getWaivedComponentUpgradeConfigUrl(), stageToSave)
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return data;
      })
      .catch(rejectWithValue);
  }
);

const saveUpgradeStageRequested = (state) => ({
  ...state,
  submitMaskState: false,
  submitError: null,
});

const saveUpgradeStageFailed = (state, { payload }) => ({
  ...state,
  submitMaskState: null,
  submitError: Messages.getHttpErrorMessage(payload),
});

const saveUpgradeStageFulfilled = (state, { payload }) => ({
  ...state,
  isDirty: false,
  submitError: null,
  submitMaskState: true,
  configuredStage: payload.waivedComponentUpgradeStageTypeId,
});

const resetSubmitMask = (state) => {
  return {
    ...state,
    submitMaskState: null,
  };
};

const waivedComponentUpgradesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setIsDirty: propSet('isDirty'),
    setConfiguredStage: propSet('configuredStage'),
    saveMaskTimerDone: resetSubmitMask,
  },
  extraReducers: {
    [saveUpgradeStage.pending]: saveUpgradeStageRequested,
    [saveUpgradeStage.fulfilled]: saveUpgradeStageFulfilled,
    [saveUpgradeStage.rejected]: saveUpgradeStageFailed,

    [loadUpgradeStage.pending]: getUpgradeStageRequested,
    [loadUpgradeStage.fulfilled]: getUpgradeStageFulfilled,
    [loadUpgradeStage.rejected]: getUpgradeStageFailed,
  },
});

export default waivedComponentUpgradesSlice.reducer;

export const actions = {
  ...waivedComponentUpgradesSlice.actions,
  saveUpgradeStage,
  loadUpgradeStage,
};
