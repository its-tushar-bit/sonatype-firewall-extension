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
import { Messages } from 'MainRoot/utilAngular/CommonServices';
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

import { always, prop } from 'ramda';
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

const toggleValue = (state, { payload: property }) => {
  state.sourceControl[property].value = !state.sourceControl[property].value;
  state.isDirty = setIsDirty(state);
};

const setValue = (state, { payload: { property, val } }) => {
  state.sourceControl[property].value = val;
  state.isDirty = setIsDirty(state);
};

const setIsInherited = (state, { payload: { property, val } }) => {
  state.sourceControl[property].isInherited = val;
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
    toggleValue,
    setValue,
    setIsInherited,
    showResetModal,
    closeResetModal,
    showConfirmUpdateModal,
    closeConfirmUpdateModal,
    setLoading,
    saveMaskTimerDone: propSet('submitMaskState', null),
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
    [UI_ROUTER_ON_FINISH]: always(initialState),
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
