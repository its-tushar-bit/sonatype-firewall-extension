/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { getGitHubAppsListUrl, getGitHubAppDeleteUrl } from '../../util/CLMLocation';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { selectOwnerInfo } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export const checkEditPermission = createAsyncThunk(
  'manageGitHubApps/checkEditPermission',
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const ownerInfo = selectOwnerInfo(state);
    const selectedOwner = selectSelectedOwner(state);
    if (!selectedOwner?.id) return rejectWithValue('No owner selected');
    return checkPermissions(['WRITE'], ownerInfo.ownerType, selectedOwner.id).catch(rejectWithValue);
  }
);

export const fetchGitHubApps = createAsyncThunk(
  'manageGitHubApps/fetchGitHubApps',
  async (ownerId, { rejectWithValue }) => {
    try {
      const response = await axios.get(getGitHubAppsListUrl(ownerId));
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const deleteGitHubApp = createAsyncThunk(
  'manageGitHubApps/deleteGitHubApp',
  async ({ githubAppId, ownerId }, { dispatch, rejectWithValue }) => {
    try {
      await axios.delete(getGitHubAppDeleteUrl(githubAppId, ownerId));
      dispatch(toastActions.addToast({ type: 'success', message: 'GitHub App removed successfully.' }));
      return githubAppId;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const initialState = Object.freeze({
  githubApps: [],
  loading: false,
  error: null,
  hasEditPermission: false,
  deleteModal: {
    isOpen: false,
    app: null,
    isDeleting: false,
  },
});

const manageGitHubAppsSlice = createSlice({
  name: 'manageGitHubApps',
  initialState,
  reducers: {
    openDeleteModal(state, action) {
      state.deleteModal.isOpen = true;
      state.deleteModal.app = action.payload;
    },
    closeDeleteModal(state) {
      state.deleteModal.isOpen = false;
      state.deleteModal.app = null;
      state.deleteModal.isDeleting = false;
    },
    resetState() {
      return initialState;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(checkEditPermission.fulfilled, (state) => {
        state.hasEditPermission = true;
      })
      .addCase(checkEditPermission.rejected, (state) => {
        state.hasEditPermission = false;
      })
      .addCase(fetchGitHubApps.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchGitHubApps.fulfilled, (state, action) => {
        state.loading = false;
        state.githubApps = action.payload;
      })
      .addCase(fetchGitHubApps.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(deleteGitHubApp.pending, (state) => {
        state.deleteModal.isDeleting = true;
      })
      .addCase(deleteGitHubApp.fulfilled, (state, action) => {
        state.deleteModal.isDeleting = false;
        state.deleteModal.isOpen = false;
        state.deleteModal.app = null;
        state.githubApps = state.githubApps.filter((app) => app.id !== action.payload);
      })
      .addCase(deleteGitHubApp.rejected, (state, action) => {
        state.deleteModal.isDeleting = false;
        state.deleteModal.isOpen = false;
        state.deleteModal.app = null;
        state.error = action.payload;
      });
  },
});

export const { openDeleteModal, closeDeleteModal, resetState } = manageGitHubAppsSlice.actions;
export default manageGitHubAppsSlice.reducer;
