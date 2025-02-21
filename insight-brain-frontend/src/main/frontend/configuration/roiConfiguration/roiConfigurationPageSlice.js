/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import * as R from 'ramda';

import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { getRoiConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'roiConfigurationPage';

const CONFIGURATION_PROPERTIES = Object.freeze([
  'baselineDaysToResolveViolation',
  'dailyRiskCostOfUnfixedViolation',
  'malwareAttacksPrevented',
  'namespaceAttacksPrevented',
  'safeComponentsAutoSelected',
]);

const mapPayloadToConfiguration = R.pick(CONFIGURATION_PROPERTIES);

export const initialConfiguration = R.zipObj(CONFIGURATION_PROPERTIES, R.repeat(0, CONFIGURATION_PROPERTIES.length));

export const initialState = Object.freeze({
  loading: true,
  error: null,
  configuration: { ...initialConfiguration },
});

const loadConfigurationRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadConfigurationRejected = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const loadConfigurationFulfilled = (state, { payload }) => {
  state.loading = false;
  state.error = null;
  state.configuration = mapPayloadToConfiguration(payload);
};

const loadConfiguration = createAsyncThunk(`${REDUCER_NAME}/loadConfiguration`, async (_, { rejectWithValue }) => {
  try {
    await checkPermissions(['CONFIGURE_SYSTEM']);
    const { data } = await axios.get(getRoiConfigurationUrl('usd'));
    return data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

const roiConfiguraionPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: {
    [loadConfiguration.pending]: loadConfigurationRequested,
    [loadConfiguration.rejected]: loadConfigurationRejected,
    [loadConfiguration.fulfilled]: loadConfigurationFulfilled,
  },
});

export default roiConfiguraionPageSlice.reducer;

export const actions = {
  ...roiConfiguraionPageSlice.actions,
  loadConfiguration,
};
