/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/util/CommonServices';
import { getContainerImageAllRepositoriesWaiversUrl } from 'MainRoot/util/CLMLocation';
import { setShowLimitedFirewallAccessAlert } from 'MainRoot/firewall/firewallActions';

const REDUCER_NAME = 'containerImageWaivers';

export const initialState = {
  loading: false,
  error: null,
  waivers: [],
};

const loadContainerImageWaivers = createAsyncThunk(
  `${REDUCER_NAME}/loadContainerImageWaivers`,
  async (_, { dispatch, rejectWithValue }) => {
    try {
      const response = await axios.get(getContainerImageAllRepositoriesWaiversUrl());
      return response.data;
    } catch (error) {
      if (error?.response?.status === 403) {
        dispatch(setShowLimitedFirewallAccessAlert(true));
        return rejectWithValue({ limitedAccess: true });
      }
      dispatch(setShowLimitedFirewallAccessAlert(false));
      return rejectWithValue(error);
    }
  }
);

const containerImageWaiversSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(loadContainerImageWaivers.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loadContainerImageWaivers.fulfilled, (state, { payload }) => {
        state.loading = false;
        state.waivers = payload;
      })
      .addCase(loadContainerImageWaivers.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = payload?.limitedAccess ? null : Messages.getHttpErrorMessage(payload);
      });
  },
});

export const actions = {
  ...containerImageWaiversSlice.actions,
  loadContainerImageWaivers,
};

export default containerImageWaiversSlice.reducer;
