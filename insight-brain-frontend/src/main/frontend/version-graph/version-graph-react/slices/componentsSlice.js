/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { find, path, pathEq, prop } from 'ramda';
import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';

import { fetchComponentDetails } from './componentDetailsSlice';
import { getVersionGraphUrl } from 'MainRoot/util/CLMLocation';

const NAME = 'components';

const initialState = {
  loading: false,
  error: null,

  // the coordinates passed in externally from the window.Insight API
  currentComponentIdentifier: null,

  selectedVersion: null,

  // additional information passed in from the external API, used to augment REST requests for accurate component data
  componentProperties: {},

  // Component versions data
  allVersions: null,
};

export const fetchComponentData = createAsyncThunk(
  `${NAME}/fetchComponentData`,
  async (applicationId, { getState, rejectWithValue, dispatch }) => {
    try {
      const state = getState();
      const currentComponentIdentifier = selectCurrentComponentIdentifier(state);
      const componentProperties = selectComponentProperties(state);

      const url = getVersionGraphUrl({
        clientType: 'rm',
        ownerType: 'application',
        ownerId: applicationId,
        componentIdentifier: JSON.stringify(currentComponentIdentifier),
        hash: componentProperties.hash,
      });

      const response = await axios.get(url);

      let allVersions = response.data.allVersions || response.data.list || response.data;

      // After successfully fetching the component data, also fetch component details
      // We're not using the thunk return value, as we'll dispatch another action separately
      dispatch(
        fetchComponentDetails({
          currentComponentIdentifier: currentComponentIdentifier,
          hash: componentProperties?.hash,
          applicationId: applicationId,
        })
      );

      return allVersions;
    } catch (error) {
      return rejectWithValue(error?.message || 'Failed to fetch component versions');
    }
  },
  {
    condition: (applicationId, { getState }) => {
      const state = getState();
      const currentComponentIdentifier = selectCurrentComponentIdentifier(state);
      return !!(currentComponentIdentifier && applicationId);
    },
  }
);

const componentsSlice = createSlice({
  name: NAME,
  initialState,
  reducers: {
    setLoading(state, action) {
      state.loading = action.payload;
    },
    setError(state, action) {
      state.error = action.payload;
      state.loading = false;
    },
    clearError(state) {
      state.error = null;
    },
    setCurrentCoordinates(state, action) {
      const { componentType, coordinates, properties = {} } = action.payload;

      // Reset state
      state.error = null;
      state.selectedVersion = null;
      state.currentComponentIdentifier = { format: componentType, coordinates };

      // Set properties
      state.componentProperties = {
        hash: properties.hash ?? null,
      };

      state.allVersions = null;
    },
    setSelectedVersion(state, action) {
      state.selectedVersion = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchComponentData.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchComponentData.fulfilled, (state, action) => {
        if (action.payload) {
          state.allVersions = action.payload;
        }
        state.loading = false;
      })
      .addCase(fetchComponentData.rejected, (state, action) => {
        state.error = action.payload;
        state.loading = false;
      });
  },
});

export const { setLoading, setError, clearError, setCurrentCoordinates, setSelectedVersion } = componentsSlice.actions;

export const selectComponentsSlice = prop(NAME);

export const selectCurrentComponentIdentifier = createSelector(
  selectComponentsSlice,
  prop('currentComponentIdentifier')
);
export const selectCurrentVersion = createSelector(selectCurrentComponentIdentifier, path(['coordinates', 'version']));

export const selectComponentProperties = createSelector(selectComponentsSlice, prop('componentProperties'));
export const selectAllVersions = createSelector(selectComponentsSlice, prop('allVersions'));
export const selectError = createSelector(selectComponentsSlice, prop('error'));
export const selectLoading = createSelector(selectComponentsSlice, prop('loading'));

// Selects the version in the graph that the user has most recently clicked on, or the current version if they haven't
// clicked any yet
export const selectSelectedVersion = createSelector(
  selectComponentsSlice,
  (state) => state.selectedVersion ?? state.currentComponentIdentifier?.coordinates?.version
);

export const selectCurrentVersionIsSelected = createSelector(
  selectSelectedVersion,
  selectCurrentVersion,
  (selectedVersion, currentVersion) => {
    return selectedVersion === currentVersion;
  }
);

export const selectSelectedComponent = createSelector(
  selectSelectedVersion,
  selectAllVersions,
  (version, allVersions) => find(pathEq(['componentIdentifier', 'coordinates', 'version'], version), allVersions)
);

export default componentsSlice.reducer;
