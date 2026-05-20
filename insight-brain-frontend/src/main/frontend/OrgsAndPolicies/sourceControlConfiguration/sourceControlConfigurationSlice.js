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
  getCompositeGitHubAppState,
  getGitHubAppReturnParam,
  getScmFormStateStorageKey,
  loadFormStateWithFallback,
  removeFormStateWithFallback,
  AUTHENTICATION_TYPES,
  hasConfiguredGitHubApp,
  selectMatchedGitHubAppInfo,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import {
  selectIsAutomationSupported,
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
  hasPendingGitHubAppReturn: false,
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

  // When changing provider, check if inherited token/username are incompatible
  // If provider is NOT inherited (local override) but token/username ARE inherited,
  // and the parent has a different provider, we must un-inherit token/username
  // to avoid cross-provider validation errors
  const parentProvider = state.serverSourceControl?.provider?.parentValue?.value;
  const isProviderInherited = state.sourceControl.provider.isInherited;
  const isTokenInherited = state.sourceControl.token.isInherited;
  const isUsernameInherited = state.sourceControl.username.isInherited;

  if (!isProviderInherited && parentProvider && parentProvider !== payload) {
    // Provider is overridden locally and parent has a different provider
    // Un-inherit token/username to avoid cross-provider validation errors
    if (isTokenInherited) {
      state.sourceControl.token.isInherited = false;
    }
    if (isUsernameInherited) {
      state.sourceControl.username.isInherited = false;
    }
  }

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
  state.isRepoUrlDirty = setIsRepoUrlDirty(state);
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
    // Sync githubApps.isInherited to match (UI has single toggle for both)
    state.sourceControl.githubApps.isInherited = false;

    if (val === 'PAT') {
      const tokenValue = state.sourceControl.token.rscValue.value;
      const usernameValue = state.sourceControl.username.rscValue.value;
      const currentProvider = state.sourceControl.provider.isInherited
        ? state.serverSourceControl?.provider?.parentValue?.value
        : state.sourceControl.provider.rscValue.value;

      state.sourceControl.token.rscValue = userInput(
        () => textFieldValidator(tokenValue, TOKEN_INPUT_MAX_CHARACTERS),
        tokenValue
      );

      // For GitHub PAT, username is NOT required - clear validation to prevent backend error
      // For Bitbucket/Azure PAT, username IS required - add validation
      const isProviderWithUsername = PROVIDERS_WITH_USERNAME.includes(currentProvider);
      if (isProviderWithUsername) {
        state.sourceControl.username.rscValue = userInput(
          () => textFieldValidator(usernameValue, USERNAME_INPUT_MAX_CHARACTERS),
          usernameValue
        );
      } else {
        // GitHub/GitLab don't use username - clear validation and value to prevent backend error
        state.sourceControl.username.rscValue = userInput(null, '');
      }
    } else if (val === AUTHENTICATION_TYPES.GITHUB_APP) {
      // When switching to GitHub App, don't inherit token/username (GitHub App doesn't need them)
      state.sourceControl.token.isInherited = false;
      state.sourceControl.username.isInherited = false;

      // Clear validation errors by resetting fields with null validator (removes validation requirements)
      const tokenValue = state.sourceControl.token.rscValue.value;
      const usernameValue = state.sourceControl.username.rscValue.value;
      state.sourceControl.token.rscValue = userInput(null, tokenValue);
      state.sourceControl.username.rscValue = userInput(null, usernameValue);
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
      state.sourceControl.githubApps.value = null;
      state.sourceControl.githubApps.isInherited = val;
    }

    // For GitHub provider: sync authenticationType and githubApps inheritance
    if (effectiveProvider === 'github') {
      state.sourceControl.authenticationType.isInherited = val;
      state.sourceControl.githubApps.isInherited = val;
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

  // Sync authenticationType and githubApps inheritance (UI has single toggle for both)
  if (property === 'authenticationType') {
    state.sourceControl.githubApps.isInherited = val;
    if (val) {
      // Inheriting - clear local value
      state.sourceControl.authenticationType.value = null;
    } else {
      // Overriding - set default value to avoid validation error
      // Use saved local value if available, otherwise default to PAT
      const savedLocalValue = state.serverSourceControl?.authenticationType?.value;
      const defaultValue = savedLocalValue || AUTHENTICATION_TYPES.PAT;
      state.sourceControl.authenticationType.value = defaultValue;
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

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (options = {}, { rejectWithValue, dispatch }) => {
  const promises = [
    dispatch(rootActions.loadSelectedOwner()),
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
  ];
  return Promise.all(promises)
    .then(() => {
      return dispatch(actions.loadSCMRootConfig(options));
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

const loadSCMRootConfig = createAsyncThunk(
  `${REDUCER_NAME}/loadSCMRootConfig`,
  (options = {}, { getState, rejectWithValue }) => {
    const state = getState();
    const ignoreGitHubAppReturn = Boolean(options?.ignoreGitHubAppReturn);
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
    const githubAppId = ignoreGitHubAppReturn ? null : getGitHubAppReturnParam(routerParams);
    const hasGitHubAppReturnParam = Boolean(githubAppId);
    const ownerType = isApp ? 'application' : 'organization';
    const promises = [
      axios.get(getCompositeSourceControlUrl(ownerType, owner.id)),
      axios.get(getSourceControlMetricsUrl(ownerType, owner.id)),
    ];
    return axios
      .all(promises)
      .then(([sourceControlData, sourceControlMetrics]) => {
        const compositeSourceControl = sourceControlData?.data ?? {};
        const compositeGitHubApp = getCompositeGitHubAppState(compositeSourceControl);
        const dirtySourceControl = compositeSourceControlToModel(compositeSourceControl, isRootOrg);
        const pendingGitHubApp = hasGitHubAppReturnParam
          ? selectMatchedGitHubAppInfo(compositeGitHubApp.value, githubAppId)
          : null;

        let originalSourceControl = { ...dirtySourceControl };
        // username and token can be inherited at org level if they are not set in the parent
        // however they cannot be inherited at app level when they are not set, so the inherit property is forced to false in that case
        dirtySourceControl.username.isInherited =
          dirtySourceControl.username.isInherited &&
          !isUsernameRequiredOnNode(dirtySourceControl, originalSourceControl, isApp) &&
          providerNeedsUsername(dirtySourceControl, originalSourceControl);
        dirtySourceControl.token.isInherited =
          dirtySourceControl.token?.isInherited &&
          !isAccessTokenRequiredOnNode(dirtySourceControl, originalSourceControl, isApp);
        dirtySourceControl.provider.isInherited = dirtySourceControl.provider.isInherited && !isRootOrg;
        originalSourceControl = { ...dirtySourceControl };

        // Generate storage key in thunk to avoid passing redundant ownerId/ownerType through payload
        const storageKey = getScmFormStateStorageKey(ownerType, owner.id);
        const savedState = consumeSavedSourceControlState(storageKey);

        return {
          sourceControl: dirtySourceControl,
          sourceControlMetrics: sourceControlMetrics.data,
          serverSourceControl: originalSourceControl,
          savedState,
          shouldShowPendingGitHubApp: hasGitHubAppReturnParam,
          pendingGitHubApp,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadSCMRootConfigPending = (state) => {
  state.formLoading = true;
  state.loadError = null;
  state.hasPendingGitHubAppReturn = false;
};

// Clone nested githubApp state so sourceControl and serverSourceControl can diverge
// without sharing value/parentValue object references during restore and dirty-state calculation.
const cloneGitHubAppState = (githubApp) => ({
  value: githubApp?.value ? { ...githubApp.value } : null,
  isInherited: githubApp?.isInherited ?? false,
  parentValue: githubApp?.parentValue ? { ...githubApp.parentValue } : null,
  parentName: githubApp?.parentName ?? null,
  localCount: githubApp?.localCount ?? 0,
  parentCount: githubApp?.parentCount ?? 0,
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
  if (!savedState) {
    return {
      sessionWasRestored: false,
    };
  }

  state.sourceControl = { ...state.sourceControl, ...savedState };

  return {
    sessionWasRestored: true,
  };
};

const deriveGitHubAppVisibilityContext = (serverSourceControl, shouldShowPendingGitHubApp, pendingGitHubApp) => {
  const backendHasLocalGithubApp = hasConfiguredGitHubApp(serverSourceControl?.githubApps?.value);
  const committedProviderValue = effectiveProvider(serverSourceControl, serverSourceControl);
  const committedEffectiveAuthType = effectiveAuthenticationType(serverSourceControl);
  const hasCommittedLocalGitHubApp =
    committedProviderValue === 'github' &&
    !serverSourceControl?.authenticationType?.isInherited &&
    (committedEffectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP || committedEffectiveAuthType === null) &&
    backendHasLocalGithubApp;
  const hasPendingLocalGitHubApp = shouldShowPendingGitHubApp && hasConfiguredGitHubApp(pendingGitHubApp);
  const visibleGitHubApp = hasPendingLocalGitHubApp
    ? pendingGitHubApp
    : hasCommittedLocalGitHubApp
    ? serverSourceControl?.githubApps?.value
    : null;

  return {
    backendHasLocalGithubApp,
    committedProviderValue,
    committedEffectiveAuthType,
    hasCommittedLocalGitHubApp,
    hasPendingLocalGitHubApp,
    visibleGitHubApp,
  };
};

const applyGitHubAppVisibilityState = (state, githubAppVisibility) => {
  const {
    backendHasLocalGithubApp,
    committedProviderValue,
    committedEffectiveAuthType,
    hasCommittedLocalGitHubApp,
    visibleGitHubApp,
  } = githubAppVisibility;
  const shouldShowLocalGithubApp = hasConfiguredGitHubApp(visibleGitHubApp);

  // Keep only committed GitHub App state by default.
  // Local backend GitHub App installs are surfaced only for committed local auth or the post-install success return.
  if (backendHasLocalGithubApp && !hasCommittedLocalGitHubApp) {
    state.serverSourceControl.githubApps = {
      ...cloneGitHubAppState(state.serverSourceControl.githubApps),
      value: null,
      isInherited: Boolean(
        committedProviderValue === 'github' && committedEffectiveAuthType === AUTHENTICATION_TYPES.GITHUB_APP
      ),
    };
  }

  if (shouldShowLocalGithubApp) {
    state.sourceControl.githubApps = {
      ...cloneGitHubAppState(state.sourceControl.githubApps),
      value: { ...visibleGitHubApp },
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

    // When backend has local apps but authenticationType was never explicitly saved,
    // sync both sides to GITHUB_APP so the component's useEffect doesn't create a mismatch.
    if (backendHasLocalGithubApp && !state.serverSourceControl.authenticationType?.value &&
        !state.serverSourceControl.authenticationType?.isInherited) {
      state.sourceControl.authenticationType = {
        ...state.sourceControl.authenticationType,
        value: AUTHENTICATION_TYPES.GITHUB_APP,
        isInherited: false,
      };
      state.serverSourceControl.authenticationType = {
        ...state.serverSourceControl.authenticationType,
        value: AUTHENTICATION_TYPES.GITHUB_APP,
        isInherited: false,
      };
    }

    return;
  }

  if (backendHasLocalGithubApp) {
    state.sourceControl.githubApps = {
      ...cloneGitHubAppState(state.sourceControl.githubApps),
      value: null,
      isInherited: Boolean(state.sourceControl?.authenticationType?.isInherited),
    };
  }
};

const finalizeDirtyStateAfterLoad = (state, sessionRestore, githubAppVisibility) => {
  const { sessionWasRestored } = sessionRestore;
  const { hasPendingLocalGitHubApp } = githubAppVisibility;

  if (!sessionWasRestored && !hasPendingLocalGitHubApp) {
    return false;
  }

  return setIsDirty(state);
};

const loadSCMRootConfigFulfilled = (
  state,
  {
    payload: {
      sourceControl,
      sourceControlMetrics,
      serverSourceControl,
      savedState,
      shouldShowPendingGitHubApp,
      pendingGitHubApp,
    },
  }
) => {
  initializeLoadedSourceControlState(state, sourceControl, serverSourceControl, sourceControlMetrics);

  // PHASE 1: Session Restore - preserves draft changes across the GitHub App registration redirect
  const sessionRestore = restoreSavedSourceControlState(state, savedState);
  const githubAppVisibility = deriveGitHubAppVisibilityContext(
    serverSourceControl,
    shouldShowPendingGitHubApp,
    pendingGitHubApp
  );

  // PHASE 2: Determine which GitHub App state should be visible after load.
  applyGitHubAppVisibilityState(state, githubAppVisibility);
  state.hasPendingGitHubAppReturn = githubAppVisibility.hasPendingLocalGitHubApp;

  // PHASE 3: Sync baseline state and compute whether the form still has unsaved changes.
  state.isDirty = finalizeDirtyStateAfterLoad(state, sessionRestore, githubAppVisibility);
};

const loadSCMRootConfigFailed = (state, { payload }) => {
  state.formLoading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.hasPendingGitHubAppReturn = false;
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
    await dispatch(actions.load({ ignoreGitHubAppReturn: true }));
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
  const storageKey = getScmFormStateStorageKey(ownerType, owner.id);

  removeFormStateWithFallback(storageKey);

  return axios
    .delete(getSourceControlUrl(ownerType, owner.id))
    .then(() => dispatch(actions.load({ ignoreGitHubAppReturn: true })).unwrap())
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
    },
    closeGitHubAppSuccessModal: (state) => {
      state.showGitHubAppSuccessModal = false;
    },
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
