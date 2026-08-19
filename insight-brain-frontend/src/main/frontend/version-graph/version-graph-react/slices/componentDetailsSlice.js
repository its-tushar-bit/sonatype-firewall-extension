/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';

import { getComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { pathSet } from 'MainRoot/util/jsUtil';

const NAME = 'componentDetails';

const initialState = {
  componentDetails: null,
  loading: false,
  error: null,
  retryParams: null,
};

export const fetchComponentDetails = createAsyncThunk(
  `${NAME}/fetchComponentDetails`,
  async (params, { dispatch, getState, rejectWithValue }) => {
    const { currentComponentIdentifier, hash, selectedVersion, applicationId } = params;

    dispatch(componentDetailsSlice.actions.setRetryParams(params));

    try {
      const componentIdentifier = selectedVersion
        ? pathSet(['coordinates', 'version'], selectedVersion, currentComponentIdentifier)
        : currentComponentIdentifier;
      const url = getComponentDetailsUrl({
        clientType: 'rm',
        ownerType: 'application',
        ownerId: applicationId,
        componentIdentifier: JSON.stringify(componentIdentifier),

        // Only pass the hash if current version is selected
        hash: selectedVersion && selectedVersion === currentComponentIdentifier.coordinates.version ? hash : null,
      });

      const response = await axios.get(url);

      let componentDetails = response.data;

      if (componentDetails.securityVulnerabilities) {
        // Remove 'Not Applicable' vulnerabilities
        const filteredVulns = componentDetails.securityVulnerabilities.filter(
          (vuln) => vuln.status !== 'Not Applicable'
        );

        // Sort by severity desc
        filteredVulns.sort((a, b) => {
          if (a.severity === b.severity) return 0;
          if (a.severity === null) return 1;
          if (b.severity === null) return -1;
          return b.severity - a.severity;
        });

        componentDetails.securityVulnerabilities = filteredVulns;
      }

      // Sort policy alerts
      if (componentDetails.policyAlerts) {
        componentDetails.policyAlerts.sort((a, b) => b.trigger.threatLevel - a.trigger.threatLevel);
      }

      return componentDetails;
    } catch (error) {
      return rejectWithValue(error?.message || 'Failed to fetch component details');
    }
  }
);

export function retryFetchComponentDetails() {
  return (dispatch, getState) => {
    const retryParams = selectRetryParams(getState());

    if (retryParams) {
      return dispatch(fetchComponentDetails(retryParams));
    }
  };
}

const componentDetailsSlice = createSlice({
  name: NAME,
  initialState,
  reducers: {
    setRetryParams: (state, action) => {
      state.retryParams = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchComponentDetails.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchComponentDetails.fulfilled, (state, action) => {
        if (action.payload) {
          state.componentDetails = action.payload;
        }
        state.loading = false;
      })
      .addCase(fetchComponentDetails.rejected, (state, action) => {
        state.error = action.payload;
        state.loading = false;
      });
  },
});

export const selectComponentDetailsSlice = prop(NAME);

export const selectLoading = createSelector(selectComponentDetailsSlice, prop('loading'));
export const selectError = createSelector(selectComponentDetailsSlice, prop('error'));
export const selectComponentDetails = createSelector(selectComponentDetailsSlice, prop('componentDetails'));

const selectRetryParams = createSelector(selectComponentDetailsSlice, prop('retryParams'));

export default componentDetailsSlice.reducer;
