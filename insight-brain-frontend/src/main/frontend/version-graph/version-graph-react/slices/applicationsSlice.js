/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { findIndex, map, pipe, prop, propEq, sortBy, toLower, toPairs } from 'ramda';
import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';

import { fetchComponentData } from './componentsSlice';
import { getApplicationNamesUrl } from 'MainRoot/util/CLMLocation';

const NAME = 'applications';

const initialState = {
  applications: [],
  selectedApplicationIndex: null,
  loading: false,
  error: null,
};

export const fetchApplications = createAsyncThunk(`${NAME}/fetchApplications`, async (_, { rejectWithValue }) => {
  try {
    const response = await axios.get(getApplicationNamesUrl());
    return response.data ?? [];
  } catch (error) {
    return rejectWithValue(error?.message || 'Failed to fetch applications');
  }
});

/**
 * Sets the application ID as specified and fetches the component information
 */
export function setApplication(applicationId) {
  return async (dispatch) => {
    dispatch(setSelectedApplicationId(applicationId));
    await dispatch(fetchComponentData(applicationId));
  };
}

const applicationsSlice = createSlice({
  name: NAME,
  initialState,
  reducers: {
    setSelectedApplicationId: (state, action) => {
      const index = findIndex(propEq('publicId', action.payload), state.applications);
      if (index === -1) {
        throw 'Application not found with publicId ' + action.payload;
      } else {
        state.selectedApplicationIndex = index;
      }
    },
    clearApplicationError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchApplications.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchApplications.fulfilled, (state, action) => {
        state.loading = false;

        state.applications = pipe(
          toPairs,
          map(([publicId, name]) => ({ publicId, name })),
          sortBy(pipe(prop('name'), toLower))
        )(action.payload);
      })
      .addCase(fetchApplications.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  },
});

export const { setSelectedApplicationId, clearApplicationError } = applicationsSlice.actions;

export const selectApplicationsSlice = prop(NAME);
export const selectApplications = createSelector(selectApplicationsSlice, prop('applications'));
export const selectLoading = createSelector(selectApplicationsSlice, prop('loading'));
export const selectError = createSelector(selectApplicationsSlice, prop('error'));

export const selectSelectedApplication = createSelector(selectApplicationsSlice, (state) => {
  const index = state.selectedApplicationIndex;
  return index !== null ? state.applications[index] : null;
});

export default applicationsSlice.reducer;
