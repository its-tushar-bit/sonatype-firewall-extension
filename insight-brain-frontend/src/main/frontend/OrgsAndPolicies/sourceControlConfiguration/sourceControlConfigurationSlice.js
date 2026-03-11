/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  getCompositeSourceControlUrl,
  getSourceControlMetricsUrl,
  getSourceControlUrl,
  getValidateScmConfigButtonUrl,
} from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import { selectIsApplication, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  compositeSourceControlToModel,
  getDataFromSourceControl,
  isAccessTokenRequiredOnNode,
  isUsernameRequiredOnNode,
  prepareSubmitData,
  providerNeedsUsername,
  setIsDirty,
  setIsRepoUrlDirty,
  textFieldValidator,
  PROVIDERS_WITH_USERNAME,
  TOKEN_INPUT_MAX_CHARACTERS,
  USERNAME_INPUT_MAX_CHARACTERS,
  BRANCH_INPUT_MAX_CHARACTERS,
  urlFieldValidator,
  getScmFormStateStorageKey,
  loadFormStateWithFallback,
  removeFormStateWithFallback,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import {
  selectIsAutomationSupported,
  selectIsGithubAppAuthenticationEnabled,
  selectIsSourceControlForSourceTileSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { userInput as selectUserInput } from '@sonatype/react-shared-components/components/NxFormSelect/stateHelpers';
import { propSet } from 'MainRoot/util/jsUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';

import { prop } from 'ramda';
const REDUCER_NAME = 'sourceControl';

export const initialState = {
  formLoading: true,
  loadError: null,
  submitError: null,
  submitMaskState: null,
  resetSubmitError: null,
  sourceControl: null,
  serverSourceControl: null,
  sourceControlMetrics: undefined,
  scmConfigValidation: {
    result: null,
    error: null,
    loading: false,
  },
  isShowAccessTokenWarning: null,
  isResetModalOpen: false,
  isConfirmationModalOpen: false,
  isDirty: false,
  isRepoUrlDirty: false,
  showGitHubAppSuccessModal: false,
  showGitHubAppReplacedAlert: false,
  isGitHubAppReplacement: false,
  isReplaceGitHubAppModalOpen: false,
};

const setProvider = (state, { payload }) => {
  const {
    sourceControl: {
      token: {
        rscValue: { value: tokenValue },
      },
      username: {
        rscValue: { value: usernameValue },
      },
    },
  } = state;
  const isProviderWithUsername = PROVIDERS_WITH_USERNAME.includes(payload);
  state.sourceControl.provider.rscValue = selectUserInput(payload, () => validateNonEmpty(payload));

  if (isProviderWithUsername) {
    state.sourceControl.username.rscValue = userInput(
      () => textFieldValidator(usernameValue, USERNAME_INPUT_MAX_CHARACTERS),
      usernameValue
    );
  } else {
    state.sourceControl.username.rscValue = userInput(null, usernameValue);
  }
  state.sourceControl.token.rscValue = userInput(
    () => textFieldValidator(tokenValue, TOKEN_INPUT_MAX_CHARACTERS),
    tokenValue
  );
  state.submitError = null;
  state.isDirty = setIsDirty(state);
};

const setUsername = (state, { payload }) => {
  state.sourceControl.username.rscValue = userInput(
    () => textFieldValidator(payload, USERNAME_INPUT_MAX_CHARACTERS),
    payload
  );
  state.isDirty = setIsDirty(state);
};

const setToken = (state, { payload }) => {
  state.sourceControl.token.rscValue = userInput(
    () => textFieldValidator(payload, TOKEN_INPUT_MAX_CHARACTERS),
    payload
  );
  state.isDirty = setIsDirty(state);
};

const setRepositoryUrl = (state, { payload }) => {
  const newRepoUrl = userInput(() => urlFieldValidator(payload), payload);
  state.sourceControl.repositoryUrl = newRepoUrl;
  state.isDirty = setIsDirty(state);
  state.isRepoUrlDirty = setIsRepoUrlDirty(state);
};

const setBaseBranch = (state, { payload }) => {
  state.sourceControl.baseBranch.rscValue = userInput(
    () => textFieldValidator(payload, BRANCH_INPUT_MAX_CHARACTERS),
    payload
  );
  state.isDirty = setIsDirty(state);
};

const setClosePrAfterDaysOpen = (state, { payload }) => {
  state.sourceControl.closePrAfterDays.rscValue = userInput(() => daysValidator(payload), payload);
  state.isDirty = setIsDirty(state);
};

const daysValidator = (val) => (val > 0 && val < 3650 ? null : 'Must be a number between 0 and 3650');

const toggleValue = (state, { payload: property }) => {
  state.sourceControl[property].value = !state.sourceControl[property].value;

  if (property === 'closePrAfterDaysOpenEnabled' && !state.sourceControl.closePrAfterDays.value) {
    state.sourceControl.closePrAfterDays.rscValue = userInput(() => daysValidator(''), '');
  }

  state.isDirty = setIsDirty(state);
};

const setValue = (state, { payload: { property, val } }) => {
  state.sourceControl[property].value = val;

  // When authentication type changes, mark it as not inherited and re-validate token if PAT
  if (property === 'authenticationType') {
    state.sourceControl[property].isInherited = false;
    // Sync githubApp.isInherited to match (UI has single toggle for both)
    state.sourceControl.githubApp.isInherited = false;

    if (val === 'PAT') {
      const tokenValue = state.sourceControl.token.rscValue.value;
      state.sourceControl.token.rscValue = userInput(
        () => textFieldValidator(tokenValue, TOKEN_INPUT_MAX_CHARACTERS),
        tokenValue
      );
    }
  }

  state.isDirty = setIsDirty(state);
};

const setIsInherited = (state, { payload: { property, val } }) => {
  state.sourceControl[property].isInherited = val;

  // Sync provider inheritance to related fields for non-GitHub providers only
  if (property === 'provider') {
    const effectiveProvider = val
      ? state.serverSourceControl?.provider?.parentValue?.value
      : state.sourceControl.provider.rscValue.value;

    if (effectiveProvider !== 'github') {
      state.sourceControl.authenticationType.value = null;
      state.sourceControl.authenticationType.isInherited = val;
      state.sourceControl.githubApp.value = null;
      state.sourceControl.githubApp.isInherited = val;
    }
  }

  // Sync authenticationType and githubApp inheritance (UI has single toggle for both)
  if (property === 'authenticationType') {
    state.sourceControl.githubApp.isInherited = val;
    if (val) {
      state.sourceControl.authenticationType.value = null;
    }
  }

  state.isDirty = setIsDirty(state);
};

const showResetModal = (state) => {
  state.isResetModalOpen = true;
};

const closeResetModal = (state) => {
  state.isResetModalOpen = false;
  state.resetSubmitError = null;
};

const showConfirmUpdateModal = (state) => {
  state.isConfirmationModalOpen = true;
};

const closeConfirmUpdateModal = (state) => {
  state.isConfirmationModalOpen = false;
};

const setLoading = (state, { payload }) => {
  state.formLoading = payload;
};

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue, dispatch }) => {
  const promises = [
    dispatch(rootActions.loadSelectedOwner()),
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
  ];
  return Promise.all(promises)
    .then(() => {
      return dispatch(actions.loadSCMRootConfig());
    })
    .catch(rejectWithValue);
});

const loadPending = (state) => {
  state.formLoading = true;
  state.loadError = null;
};

const loadFailed = (state, { payload }) => {
  state.formLoading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);

  // Defensive cleanup: Remove temporary field if load failed
  if (state.serverSourceControl?._originalAuthType !== undefined) {
    delete state.serverSourceControl._originalAuthType;
  }
};

const loadSCMRootConfig = createAsyncThunk(`${REDUCER_NAME}/loadSCMRootConfig`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const owner = selectSelectedOwner(state);
  const isSourceControlSupported = selectIsSourceControlForSourceTileSupported(state);
  if (!isSourceControlSupported || !owner) {
    return {
      sourceControl: null,
      sourceControlMetrics: undefined,
      serverSourceControl: null,
      storageKey: null,
    };
  }
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
  const isGithubAppAuthenticationEnabled = selectIsGithubAppAuthenticationEnabled(state);
  const ownerType = isApp ? 'application' : 'organization';
  const promises = [
    axios.get(getCompositeSourceControlUrl(ownerType, owner.id)),
    axios.get(getSourceControlMetricsUrl(ownerType, owner.id)),
  ];
  return axios
    .all(promises)
    .then(([sourceControlData, sourceControlMetrics]) => {
      const compositeSourceControl = sourceControlData?.data ?? {};
      const dirtySourceControl = compositeSourceControlToModel(compositeSourceControl, isRootOrg);

      let originalSourceControl = { ...dirtySourceControl };
      // username and token can be inherited at org level if they are not set in the parent
      // however they cannot be inherited at app level when they are not set, so the inherit property is forced to false in that case
      dirtySourceControl.username.isInherited =
        dirtySourceControl.username.isInherited &&
        !isUsernameRequiredOnNode(dirtySourceControl, originalSourceControl, isApp) &&
        providerNeedsUsername(dirtySourceControl, originalSourceControl);
      dirtySourceControl.token.isInherited =
        dirtySourceControl.token?.isInherited &&
        !isAccessTokenRequiredOnNode(
          dirtySourceControl,
          originalSourceControl,
          isApp,
          isGithubAppAuthenticationEnabled
        );
      dirtySourceControl.provider.isInherited = dirtySourceControl.provider.isInherited && !isRootOrg;
      originalSourceControl = { ...dirtySourceControl };

      // Generate storage key in thunk to avoid passing redundant ownerId/ownerType through payload
      const storageKey = isGithubAppAuthenticationEnabled ? getScmFormStateStorageKey(ownerType, owner.id) : null;

      return {
        sourceControl: dirtySourceControl,
        sourceControlMetrics: sourceControlMetrics.data,
        serverSourceControl: originalSourceControl,
        storageKey,
      };
    })
    .catch(rejectWithValue);
});

const loadSCMRootConfigPending = (state) => {
  state.formLoading = true;
  state.loadError = null;
};

const loadSCMRootConfigFulfilled = (
  state,
  { payload: { sourceControl, sourceControlMetrics, serverSourceControl, storageKey } }
) => {
  state.formLoading = false;
  // CRITICAL: Create independent copies to prevent payload mutation
  // Redux Toolkit's Immer allows direct mutation of state, but we must not mutate the payload
  // Without this, modifying state.sourceControl/serverSourceControl would mutate the original payload objects
  state.sourceControl = { ...sourceControl };
  state.serverSourceControl = { ...serverSourceControl };
  state.sourceControlMetrics = sourceControlMetrics;

  let hasChanges = false;
  let sessionWasRestored = false;

  // Store original authenticationType BEFORE any modifications
  // Uses this to distinguish fresh install vs reconfigure in showGitHubAppSuccessModal
  // This temporary field (_originalAuthType) is:
  // 1. Captured here from fresh backend data
  // 2. Stored in serverSourceControl during sync
  // 3. Read in showGitHubAppSuccessModal action
  // 4. Used to determine if this is a replacement
  // 5. Cleaned up after modal is shown
  const originalAuthType = serverSourceControl?.authenticationType?.value;
  // Check backend for GitHub App before session restore (serverSourceControl has fresh backend data)
  const backendHasGithubApp = serverSourceControl?.githubApp?.value?.installationId;
  // PHASE 1: Session Restore - preserves draft changes during GitHub App OAuth redirect
  let savedStateHadGithubApp = false;
  if (storageKey) {
    const savedStateJson = loadFormStateWithFallback(storageKey);
    if (savedStateJson) {
      try {
        const savedState = JSON.parse(savedStateJson);
        savedStateHadGithubApp = Boolean(savedState?.githubApp?.value?.installationId);
        state.sourceControl = { ...state.sourceControl, ...savedState };
        sessionWasRestored = true;
        hasChanges = true;
        // Set baseline state before OAuth for dirty detection
        state.serverSourceControl.githubApp = { ...savedState.githubApp };
      } catch (error) {
        console.warn('Failed to parse saved form state:', error);
      } finally {
        removeFormStateWithFallback(storageKey);
      }
    }
  }

  // PHASE 2: Apply GitHub App data from backend, overriding stale session values
  if (backendHasGithubApp) {
    state.sourceControl.githubApp = { ...serverSourceControl.githubApp };
    state.sourceControl.authenticationType = {
      ...serverSourceControl.authenticationType,
      value: 'GITHUB_APP',
      isInherited: false,
    };
    // Force provider to GitHub when GitHub App is installed
    if (state.sourceControl.provider?.rscValue?.value !== 'github') {
      const newProviderValue = selectUserInput('github', () => validateNonEmpty('github'));
      state.sourceControl.provider.rscValue = newProviderValue;
      state.sourceControl.provider.isInherited = false;
    }
  }

  // PHASE 3: Sync serverSourceControl and determine hasChanges
  state.serverSourceControl._originalAuthType = originalAuthType;

  if (!sessionWasRestored) {
    state.serverSourceControl.provider = { ...state.sourceControl.provider };
    state.serverSourceControl.authenticationType = { ...state.sourceControl.authenticationType };
    state.serverSourceControl.githubApp = { ...state.sourceControl.githubApp };
    hasChanges = false;
  } else {
    state.serverSourceControl.provider = { ...state.sourceControl.provider };
    state.serverSourceControl.authenticationType = { ...state.sourceControl.authenticationType };
    //
    // For RECONFIGURE: Backend already saved new installation during OAuth → sync to show "no changes"
    // For FRESH INSTALL: Need to detect change from null → new installation → don't sync

    if (savedStateHadGithubApp && backendHasGithubApp) {
      // RECONFIGURE: User had GitHub App before OAuth, backend saved new installation
      // Sync serverSourceControl to show "no changes" (backend already persisted it)
      state.serverSourceControl.githubApp = { ...state.sourceControl.githubApp };
    }
    // FRESH INSTALL (else): User had PAT/null before OAuth, keep baseline from PHASE 1 for dirty detection

    hasChanges = setIsDirty(state);
  }

  state.isDirty = hasChanges;
};

const loadSCMRootConfigFailed = (state, { payload }) => {
  state.formLoading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);

  // Defensive cleanup: Remove temporary field if load failed during GitHub App flow
  // Prevents memory leak if OAuth flow encounters error before modal is shown
  if (state.serverSourceControl?._originalAuthType !== undefined) {
    delete state.serverSourceControl._originalAuthType;
  }
};

const save = createAsyncThunk(`${REDUCER_NAME}/save`, async (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { sourceControl, serverSourceControl } = selectSourceControlConfigurationSlice(state);
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
  const isAutomationSupported = selectIsAutomationSupported(state);
  const owner = selectSelectedOwner(state);
  const ownerType = isApp ? 'application' : 'organization';

  try {
    const submitSourceControlData = prepareSubmitData(
      sourceControl,
      serverSourceControl,
      isApp,
      isRootOrg,
      isAutomationSupported
    );
    const data = getDataFromSourceControl(ownerType, submitSourceControlData);
    const requestType =
      (sourceControl?.id && isApp && submitSourceControlData.repositoryUrl !== serverSourceControl.repositoryUrl) ||
      sourceControl?.id
        ? 'put'
        : 'post';

    await axios[requestType](getSourceControlUrl(ownerType, owner.id), data);
    startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
    await dispatch(actions.load());
  } catch (error) {
    return rejectWithValue(error);
  }
});

const savePending = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
  state.scmConfigValidation = {
    result: null,
    error: null,
    loading: false,
  };
};

const saveFulfilled = (state) => {
  state.submitMaskState = true;
  state.submitError = null;
  state.isDirty = false;
  state.isConfirmationModalOpen = false;
};

const saveFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.isConfirmationModalOpen = false;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const reset = createAsyncThunk(`${REDUCER_NAME}/reset`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const isApp = selectIsApplication(state);
  const owner = selectSelectedOwner(state);
  const ownerType = isApp ? 'application' : 'organization';

  return axios
    .delete(getSourceControlUrl(ownerType, owner.id))
    .then(() => {
      dispatch(actions.load());
    })
    .catch(rejectWithValue);
});

const resetPending = (state) => {
  state.resetSubmitError = null;
};

const resetFulfilled = (state) => {
  state.resetSubmitError = null;
  state.submitError = null;
  state.isResetModalOpen = false;
  state.isDirty = false;
};

const resetFailed = (state, { payload }) => {
  state.resetSubmitError = Messages.getHttpErrorMessage(payload);
};

const validate = createAsyncThunk(`${REDUCER_NAME}/validate`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const isApp = selectIsApplication(state);
  const owner = selectSelectedOwner(state);
  const ownerType = isApp ? 'application' : 'organization';
  return axios.get(getValidateScmConfigButtonUrl(ownerType, owner.id)).then(prop('data')).catch(rejectWithValue);
});

const validatePending = (state) => {
  state.scmConfigValidation = {
    result: null,
    error: null,
    loading: true,
  };
};

const validateFulfilled = (state, { payload }) => {
  state.scmConfigValidation = {
    result: payload,
    error: null,
    loading: false,
  };
};

const validateFailed = (state, { payload }) => {
  state.scmConfigValidation = {
    result: null,
    error: Messages.getHttpErrorMessage(payload),
    loading: false,
  };
};

const sourceControl = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setProvider,
    setRepositoryUrl,
    setUsername,
    setToken,
    setBaseBranch,
    setClosePrAfterDaysOpen,
    toggleValue,
    setValue,
    setIsInherited,
    showResetModal,
    closeResetModal,
    showConfirmUpdateModal,
    closeConfirmUpdateModal,
    setLoading,
    saveMaskTimerDone: propSet('submitMaskState', null),
    showGitHubAppSuccessModal: (state) => {
      state.showGitHubAppSuccessModal = true;

      const authTypeToCheck =
        state.serverSourceControl._originalAuthType !== undefined
          ? state.serverSourceControl._originalAuthType
          : state.serverSourceControl?.authenticationType?.value;

      // Determine if this is a replacement: had GITHUB_APP auth before installation
      state.isGitHubAppReplacement = authTypeToCheck === 'GITHUB_APP';

      // Clean up temporary field used for integration
      delete state.serverSourceControl._originalAuthType;
    },
    closeGitHubAppSuccessModal: (state) => {
      state.showGitHubAppSuccessModal = false;

      // Only show replacement banner if this was actually a reconfiguration
      state.showGitHubAppReplacedAlert = state.isGitHubAppReplacement;

      state.isGitHubAppReplacement = false; // Reset flag
    },
    closeGitHubAppReplacedAlert: propSet('showGitHubAppReplacedAlert', false),
    openReplaceGitHubAppModal: (state) => {
      state.isReplaceGitHubAppModalOpen = true;
    },
    closeReplaceGitHubAppModal: (state) => {
      state.isReplaceGitHubAppModalOpen = false;
    },
    enableGitHubAppFeatures: (state) => {
      // Enable golden PRs if not already enabled (respects already-enabled state)
      // Also enable if currently inherited, to make it explicit at this level
      if (
        !state.sourceControl.remediationPullRequestsEnabled.value ||
        state.sourceControl.remediationPullRequestsEnabled.isInherited
      ) {
        state.sourceControl.remediationPullRequestsEnabled.value = true;
        state.sourceControl.remediationPullRequestsEnabled.isInherited = false;
      }

      // Enable manual PRs if not already enabled (respects already-enabled state)
      // Also enable if currently inherited, to make it explicit at this level
      if (
        !state.sourceControl.manualPullRequestsEnabled.value ||
        state.sourceControl.manualPullRequestsEnabled.isInherited
      ) {
        state.sourceControl.manualPullRequestsEnabled.value = true;
        state.sourceControl.manualPullRequestsEnabled.isInherited = false;
      }
      // Recalculate isDirty flag so form validation recognizes there are changes
      state.isDirty = setIsDirty(state);
    },
  },
  extraReducers: {
    [loadSCMRootConfig.pending]: loadSCMRootConfigPending,
    [loadSCMRootConfig.fulfilled]: loadSCMRootConfigFulfilled,
    [loadSCMRootConfig.rejected]: loadSCMRootConfigFailed,
    [load.pending]: loadPending,
    [load.rejected]: loadFailed,
    [save.pending]: savePending,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
    [reset.pending]: resetPending,
    [reset.fulfilled]: resetFulfilled,
    [reset.rejected]: resetFailed,
    [validate.pending]: validatePending,
    [validate.fulfilled]: validateFulfilled,
    [validate.rejected]: validateFailed,
    [UI_ROUTER_ON_FINISH]: (state) => {
      // Only reset form-specific state, preserve data and modal state
      // This avoids unnecessary object creation and preserves references
      state.formLoading = initialState.formLoading;
      state.loadError = initialState.loadError;
      state.submitError = initialState.submitError;
      state.submitMaskState = initialState.submitMaskState;
      state.resetSubmitError = initialState.resetSubmitError;
      state.scmConfigValidation = { ...initialState.scmConfigValidation };
      state.isResetModalOpen = initialState.isResetModalOpen;
      state.isConfirmationModalOpen = initialState.isConfirmationModalOpen;
      state.isDirty = initialState.isDirty;
      state.isRepoUrlDirty = initialState.isRepoUrlDirty;
      // Preserved: showGitHubAppSuccessModal, sourceControl, serverSourceControl, sourceControlMetrics
    },
  },
});

export default sourceControl.reducer;
export const actions = {
  ...sourceControl.actions,
  loadSCMRootConfig,
  load,
  save,
  reset,
  validate,
};
