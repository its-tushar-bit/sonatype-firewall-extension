/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  getCompositeSourceControlUrl,
  getSourceControlMetricsUrl,
  getSourceControlUrl,
  getValidateScmConfigButtonUrl,
} from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import {
  selectIsApplication,
  selectIsRootOrganization,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
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
  effectiveAuthenticationType,
  effectiveProvider,
  getScmFormStateStorageKey,
  loadFormStateWithFallback,
  removeFormStateWithFallback,
  AUTHENTICATION_TYPES,
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

  // Sync provider inheritance to related fields
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

    // Sync token and username inheritance with provider.
    // When provider is overridden, inherited credentials from parent are
    // incompatible with the new provider — mark them as overridden so the
    // cross-provider validation check (selectValidationError) does not
    // block save with a generic error that has no field-level indicator.
    // When provider is inherited, credentials should also be inherited.
    state.sourceControl.token.isInherited = val;
    state.sourceControl.username.isInherited = val;
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
};

const consumeSavedSourceControlState = (storageKey) => {
  if (!storageKey) {
    return null;
  }

  const savedStateJson = loadFormStateWithFallback(storageKey);
  if (!savedStateJson) {
    return null;
  }

  try {
    return JSON.parse(savedStateJson);
  } catch (error) {
    console.warn('Failed to parse saved form state:', error);
    return null;
  } finally {
    removeFormStateWithFallback(storageKey);
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
      savedState: null,
      shouldShowPendingGitHubApp: false,
    };
  }
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
  const routerParams = selectRouterCurrentParams(state);
  const isGithubAppAuthenticationEnabled = selectIsGithubAppAuthenticationEnabled(state);
  const shouldShowPendingGitHubApp = isGithubAppAuthenticationEnabled && routerParams?.githubAppSuccess === 'true';
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
      const savedState = consumeSavedSourceControlState(storageKey);

      return {
        sourceControl: dirtySourceControl,
        sourceControlMetrics: sourceControlMetrics.data,
        serverSourceControl: originalSourceControl,
        savedState,
        shouldShowPendingGitHubApp,
      };
    })
    .catch(rejectWithValue);
});

const loadSCMRootConfigPending = (state) => {
  state.formLoading = true;
  state.loadError = null;
};

// Clone nested githubApp state so sourceControl and serverSourceControl can diverge
// without sharing value/parentValue object references during restore and dirty-state calculation.
const cloneGitHubAppState = (githubApp) => ({
  value: githubApp?.value ? { ...githubApp.value } : null,
  isInherited: githubApp?.isInherited ?? false,
  parentValue: githubApp?.parentValue ? { ...githubApp.parentValue } : null,
  parentName: githubApp?.parentName ?? null,
});

const initializeLoadedSourceControlState = (state, sourceControl, serverSourceControl, sourceControlMetrics) => {
  state.formLoading = false;
  // CRITICAL: Create independent copies to prevent payload mutation
  // Redux Toolkit's Immer allows direct mutation of state, but we must not mutate the payload
  // Without this, modifying state.sourceControl/serverSourceControl would mutate the original payload objects
  state.sourceControl = { ...sourceControl };
  state.serverSourceControl = { ...serverSourceControl };
  state.sourceControlMetrics = sourceControlMetrics;
};

const restoreSavedSourceControlState = (state, savedState) => {
  const savedStateHadGithubApp = Boolean(savedState?.githubApp?.value?.installationId);

  if (!savedState) {
    return {
      savedStateHadGithubApp,
      sessionWasRestored: false,
    };
  }

  state.sourceControl = { ...state.sourceControl, ...savedState };
  // Set baseline state from before the GitHub App redirect for dirty detection
  state.serverSourceControl.githubApp = cloneGitHubAppState(savedState.githubApp);

  return {
    savedStateHadGithubApp,
    sessionWasRestored: true,
  };
};

const deriveGitHubAppVisibilityContext = (serverSourceControl, shouldShowPendingGitHubApp) => {
  const backendHasLocalGithubApp = Boolean(serverSourceControl?.githubApp?.value?.installationId);
  const committedProviderValue = effectiveProvider(serverSourceControl, serverSourceControl);
  const committedEffectiveAuthType = effectiveAuthenticationType(serverSourceControl);
  const hasCommittedLocalGitHubApp =
    committedProviderValue === 'github' &&
    !serverSourceControl?.authenticationType?.isInherited &&
    committedEffectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP;

  return {
    backendHasLocalGithubApp,
    committedProviderValue,
    committedEffectiveAuthType,
    hasCommittedLocalGitHubApp,
    shouldShowLocalGithubApp: backendHasLocalGithubApp && (hasCommittedLocalGitHubApp || shouldShowPendingGitHubApp),
    shouldShowPendingLocalGithubApp:
      backendHasLocalGithubApp && shouldShowPendingGitHubApp && !hasCommittedLocalGitHubApp,
  };
};

const applyGitHubAppVisibilityState = (state, serverSourceControl, githubAppVisibility) => {
  const {
    backendHasLocalGithubApp,
    committedProviderValue,
    committedEffectiveAuthType,
    hasCommittedLocalGitHubApp,
    shouldShowLocalGithubApp,
  } = githubAppVisibility;

  // Keep only committed GitHub App state by default.
  // Local backend GitHub App installs are surfaced only for committed local auth or the post-install success return.
  if (backendHasLocalGithubApp && !hasCommittedLocalGitHubApp) {
    state.serverSourceControl.githubApp = {
      ...cloneGitHubAppState(state.serverSourceControl.githubApp),
      value: null,
      isInherited: Boolean(
        committedProviderValue === 'github' && committedEffectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP
      ),
    };
  }

  if (shouldShowLocalGithubApp) {
    state.sourceControl.githubApp = {
      ...cloneGitHubAppState(state.sourceControl.githubApp),
      value: { ...serverSourceControl.githubApp.value },
      isInherited: false,
    };

    if (!hasCommittedLocalGitHubApp) {
      state.sourceControl.authenticationType = {
        ...state.sourceControl.authenticationType,
        value: AUTHENTICATION_TYPES.GITHUB_APP,
        isInherited: false,
      };

      if (state.sourceControl.provider?.rscValue?.value !== 'github' || state.sourceControl.provider?.isInherited) {
        state.sourceControl.provider.rscValue = selectUserInput('github', () => validateNonEmpty('github'));
        state.sourceControl.provider.isInherited = false;
      }
    }

    return;
  }

  if (backendHasLocalGithubApp) {
    state.sourceControl.githubApp = {
      ...cloneGitHubAppState(state.sourceControl.githubApp),
      value: null,
      isInherited: Boolean(state.sourceControl?.authenticationType?.isInherited),
    };
  }
};

const syncServerSourceControlCoreFields = (state) => {
  state.serverSourceControl.provider = { ...state.sourceControl.provider };
  state.serverSourceControl.authenticationType = { ...state.sourceControl.authenticationType };
};

const finalizeDirtyStateAfterLoad = (state, sessionRestore, githubAppVisibility) => {
  const { savedStateHadGithubApp, sessionWasRestored } = sessionRestore;
  const { backendHasLocalGithubApp, shouldShowPendingLocalGithubApp } = githubAppVisibility;

  syncServerSourceControlCoreFields(state);

  if (!sessionWasRestored && !shouldShowPendingLocalGithubApp) {
    state.serverSourceControl.githubApp = cloneGitHubAppState(state.sourceControl.githubApp);
    return false;
  }

  // For RECONFIGURE: Backend already saved the new installation during the GitHub App redirect
  // → sync to show "no changes"
  // For FRESH INSTALL: Need to detect change from null → new installation → don't sync

  if (savedStateHadGithubApp && backendHasLocalGithubApp) {
    // RECONFIGURE: User had GitHub App before the redirect, backend saved new installation
    // Sync serverSourceControl to show "no changes" (backend already persisted it)
    state.serverSourceControl.githubApp = cloneGitHubAppState(state.sourceControl.githubApp);
  }
  // FRESH INSTALL (else): User had PAT/null before the redirect, keep baseline from PHASE 1 for dirty detection

  return setIsDirty(state);
};

const loadSCMRootConfigFulfilled = (
  state,
  { payload: { sourceControl, sourceControlMetrics, serverSourceControl, savedState, shouldShowPendingGitHubApp } }
) => {
  initializeLoadedSourceControlState(state, sourceControl, serverSourceControl, sourceControlMetrics);

  // PHASE 1: Session Restore - preserves draft changes across the GitHub App registration redirect
  const sessionRestore = restoreSavedSourceControlState(state, savedState);
  const githubAppVisibility = deriveGitHubAppVisibilityContext(serverSourceControl, shouldShowPendingGitHubApp);

  // PHASE 2: Determine which GitHub App state should be visible after load.
  applyGitHubAppVisibilityState(state, serverSourceControl, githubAppVisibility);

  // PHASE 3: Sync baseline state and compute whether the form still has unsaved changes.
  state.isDirty = finalizeDirtyStateAfterLoad(state, sessionRestore, githubAppVisibility);
};

const loadSCMRootConfigFailed = (state, { payload }) => {
  state.formLoading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const save = createAsyncThunk(`${REDUCER_NAME}/save`, async (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { sourceControl, serverSourceControl } = selectSourceControlConfigurationSlice(state);
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
  const isAutomationSupported = selectIsAutomationSupported(state);
  const isGithubAppAuthenticationEnabled = selectIsGithubAppAuthenticationEnabled(state);
  const owner = selectSelectedOwner(state);
  const ownerType = isApp ? 'application' : 'organization';

  try {
    const submitSourceControlData = prepareSubmitData(
      sourceControl,
      serverSourceControl,
      isApp,
      isRootOrg,
      isAutomationSupported,
      isGithubAppAuthenticationEnabled
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

      // Determine if this is a replacement: check if GitHub App already existed (has installation ID)
      const hadExistingGitHubApp = state.serverSourceControl?.githubApp?.value?.installationId != null;
      state.isGitHubAppReplacement = hadExistingGitHubApp;
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
