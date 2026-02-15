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

  if (property === 'provider') {
    state.sourceControl.authenticationType.isInherited = val;
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

const loadSCMRootConfig = createAsyncThunk(`${REDUCER_NAME}/loadSCMRootConfig`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const owner = selectSelectedOwner(state);
  const isSourceControlSupported = selectIsSourceControlForSourceTileSupported(state);
  if (!isSourceControlSupported || !owner) {
    return {
      sourceControl: null,
      sourceControlMetrics: undefined,
      serverSourceControl: null,
    };
  }
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
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
        !isAccessTokenRequiredOnNode(dirtySourceControl, originalSourceControl, isApp);
      dirtySourceControl.provider.isInherited = dirtySourceControl.provider.isInherited && !isRootOrg;
      originalSourceControl = { ...dirtySourceControl };

      return {
        sourceControl: dirtySourceControl,
        sourceControlMetrics: sourceControlMetrics.data,
        serverSourceControl: originalSourceControl,
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
  { payload: { sourceControl, sourceControlMetrics, serverSourceControl } }
) => {
  state.formLoading = false;
  state.sourceControl = sourceControl;
  state.serverSourceControl = serverSourceControl;
  state.sourceControlMetrics = sourceControlMetrics;

  let hasChanges = false;

  // If GitHub App is installed, set provider and authenticationType
  if (sourceControl?.githubApp?.value?.installationId) {
    if (!sourceControl.provider?.rscValue?.value) {
      const newProviderValue = selectUserInput('github', () => validateNonEmpty('github'));
      state.sourceControl.provider.rscValue = newProviderValue;
      state.sourceControl.provider.isInherited = false;
      hasChanges = true;
    }
    if (sourceControl.authenticationType?.value !== 'GITHUB_APP') {
      state.sourceControl.authenticationType.value = 'GITHUB_APP';
      state.sourceControl.authenticationType.isInherited = false;
      hasChanges = true;
    }

    // Mark form as dirty so user can save the GitHub App configuration
    if (hasChanges) {
      state.isDirty = setIsDirty(state);
    }
  }
};

const loadSCMRootConfigFailed = (state, { payload }) => {
  state.formLoading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const save = createAsyncThunk(`${REDUCER_NAME}/save`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { sourceControl, serverSourceControl } = selectSourceControlConfigurationSlice(state);
  const isApp = selectIsApplication(state);
  const isRootOrg = selectIsRootOrganization(state);
  const isAutomationSupported = selectIsAutomationSupported(state);
  const owner = selectSelectedOwner(state);
  const ownerType = isApp ? 'application' : 'organization';
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
  return axios[requestType](getSourceControlUrl(ownerType, owner.id), data)
    .then(() => {
      startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
      dispatch(actions.load());
    })
    .catch(rejectWithValue);
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
    },
    closeGitHubAppSuccessModal: propSet('showGitHubAppSuccessModal', false),
    enableGitHubAppFeatures: (state) => {
      // Update only sourceControl so form is marked as dirty
      if (!state.sourceControl.remediationPullRequestsEnabled.value) {
        state.sourceControl.remediationPullRequestsEnabled.value = true;
      }
      if (!state.sourceControl.manualPullRequestsEnabled.value) {
        state.sourceControl.manualPullRequestsEnabled.value = true;
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
