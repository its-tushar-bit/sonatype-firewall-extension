/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { getGitHubAppManifestUrl } from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import { always } from 'ramda';

const { initialState: nxTextInputInitialState, userInput } = nxTextInputStateHelpers;

/**
 * Validator for organization name field.
 * Ensures the value is not empty or whitespace-only.
 */
const orgNameValidator = (val) => (val && val.trim().length > 0 ? null : 'Required field');

const REDUCER_NAME = 'gitHubAppConfiguration';
const GITHUB_APP_URLS = {
  ORGANIZATION_SETTINGS: 'https://github.com/organizations/{orgName}/settings/apps/new',
  PERSONAL_SETTINGS: 'https://github.com/settings/apps/new',
};

export const initialState = Object.freeze({
  setupError: null,
  registrationInProgress: false,
  isModalOpen: false,
  formState: {
    accountType: 'organization',
    organizationName: nxTextInputInitialState(''),
  },
});

/**
 * Async thunk to initiate GitHub App registration flow.
 *
 * Gets manifest from backend, then POSTs it to GitHub's app creation page.
 *
 * @param {Object} params - Registration parameters
 * @param {string} params.accountType - 'organization' or 'personal'
 * @param {string} params.organizationName - GitHub org name (required if accountType is 'organization')
 */
const initiateGitHubAppRegistration = createAsyncThunk(
  `${REDUCER_NAME}/initiateGitHubAppRegistration`,
  async ({ accountType, organizationName }, { getState, rejectWithValue }) => {
    try {
      const state = getState();
      const owner = selectSelectedOwner(state);
      const orgName = organizationName?.trimmedValue || null;
      const response = await axios.post(getGitHubAppManifestUrl(owner.id, orgName));
      const { manifest, state: stateToken } = response.data;

      const githubUrl =
        accountType === 'organization'
          ? GITHUB_APP_URLS.ORGANIZATION_SETTINGS.replace('{orgName}', orgName)
          : GITHUB_APP_URLS.PERSONAL_SETTINGS;

      // Create hidden form to POST manifest to GitHub
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = githubUrl;
      form.style.display = 'none';
      const manifestInput = document.createElement('input');
      manifestInput.type = 'hidden';
      manifestInput.name = 'manifest';
      manifestInput.value = JSON.stringify(manifest);
      form.appendChild(manifestInput);
      if (stateToken) {
        const stateInput = document.createElement('input');
        stateInput.type = 'hidden';
        stateInput.name = 'state';
        stateInput.value = stateToken;
        form.appendChild(stateInput);
      }
      document.body.appendChild(form);
      form.submit();
      return { accountType, organizationName, manifest };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const gitHubAppConfiguration = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetGitHubAppState: () => initialState,
    openModal: (state) => {
      state.isModalOpen = true;
    },
    closeModal: (state) => {
      state.isModalOpen = false;
    },
    setAccountType: (state, action) => {
      state.formState.accountType = action.payload;
      if (action.payload === 'personal') {
        state.formState.organizationName = nxTextInputInitialState('');
      }
    },
    setOrganizationName: (state, action) => {
      state.formState.organizationName = userInput(orgNameValidator, action.payload);
    },
    resetFormState: (state) => {
      state.formState = initialState.formState;
    },
  },
  extraReducers: (builder) => {
    builder
      // Initiate registration handlers
      .addCase(initiateGitHubAppRegistration.pending, (state) => {
        state.registrationInProgress = true;
        state.setupError = null;
      })
      // no initiateGitHubAppRegistration.fulfilled handler, as user gets redirected to GH at that point
      .addCase(initiateGitHubAppRegistration.rejected, (state, { payload }) => {
        state.registrationInProgress = false;
        state.setupError = Messages.getHttpErrorMessage(payload);
      })
      // Reset state on route change
      .addCase(UI_ROUTER_ON_FINISH, always(initialState));
  },
});

export const actions = {
  ...gitHubAppConfiguration.actions,
  initiateGitHubAppRegistration,
};

export default gitHubAppConfiguration.reducer;
