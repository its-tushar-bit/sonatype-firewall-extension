/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectWaiverExpirationNotificationSlice } from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSelectors';
import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { getWaiverExpirationNotificationConfigUrl, getRoleMappingForCurrentOwnerUrl } from 'MainRoot/util/CLMLocation';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { equals } from 'ramda';

const REDUCER_NAME = 'waiverExpirationNotification';

export const RECIPIENT_TYPE_DIRECT = 'DIRECT';
export const RECIPIENT_TYPE_ROLE = 'ROLE';
export const RECIPIENT_TYPE_BOTH = 'BOTH';

export const initialState = {
  serverData: null,
  notificationDays: [],
  recipientType: RECIPIENT_TYPE_DIRECT,
  directEmails: [],
  roleIds: [],
  inheritConfig: false,
  isDirty: false,
  loading: false,
  loadError: null,
  submitMaskState: null,
  submitError: null,
  availableRoles: [],
};

const goToEditor = createAsyncThunk(`${REDUCER_NAME}/goToEditor`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'edit-waiver-expiration-notification');
  dispatch(stateGo(to, params));
});

const loadRoles = createAsyncThunk(
  `${REDUCER_NAME}/loadRoles`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    return axios
      .get(getRoleMappingForCurrentOwnerUrl(ownerType, ownerId))
      .then((response) => response.data.membersByRole || [])
      .catch((err) => rejectWithValue(err));
  }
);

const loadConfig = createAsyncThunk(
  `${REDUCER_NAME}/loadConfig`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    await dispatch(rootActions.loadSelectedOwner());
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios
      .get(getWaiverExpirationNotificationConfigUrl(ownerType, ownerId))
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err));
  }
);

const loadConfigRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.submitError = null;
};

const loadConfigFulfilled = (state, { payload }) => {
  state.loading = false;
  state.isDirty = false;
  state.serverData = payload;
  state.inheritConfig = !!payload.inheritConfig;
  state.notificationDays = payload.notificationDays || [];
  state.recipientType = payload.recipientType || RECIPIENT_TYPE_DIRECT;
  state.directEmails = payload.directEmails || [];
  state.roleIds = payload.roleIds || [];
};

const loadConfigFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const deleteConfig = createAsyncThunk(
  `${REDUCER_NAME}/deleteConfig`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const router = selectRouterSlice(state);
    const { params } = deriveEditRoute(router, 'edit-waiver-expiration-notification');
    return axios
      .put(getWaiverExpirationNotificationConfigUrl(ownerType, ownerId), { inheritConfig: true })
      .then(() => dispatch(stateGo(`management.view.${ownerType}`, params)))
      .catch((error) => rejectWithValue(error));
  }
);

const saveConfig = createAsyncThunk(
  `${REDUCER_NAME}/saveConfig`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const { inheritConfig, notificationDays, directEmails, roleIds } =
      selectWaiverExpirationNotificationSlice(state);

    const hasEmails = directEmails && directEmails.length > 0;
    const hasRoles = roleIds && roleIds.length > 0;
    const derivedRecipientType =
      hasEmails && hasRoles ? RECIPIENT_TYPE_BOTH : hasRoles ? RECIPIENT_TYPE_ROLE : RECIPIENT_TYPE_DIRECT;

    const payload = {
      inheritConfig,
      notificationDays,
      recipientType: derivedRecipientType,
      directEmails,
      roleIds,
    };

    return axios
      .put(getWaiverExpirationNotificationConfigUrl(ownerType, ownerId), payload)
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone).then(() => dispatch(actions.loadConfig()));
      })
      .catch((error) => rejectWithValue(error));
  }
);

const saveConfigRequested = (state) => {
  state.submitMaskState = false;
  state.submitError = null;
};

const saveConfigFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveConfigFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const computeIsDirty = (state) => {
  if (!state.serverData) {
    state.isDirty = false;
    return;
  }
  const current = {
    inheritConfig: state.inheritConfig,
    notificationDays: state.notificationDays,
    directEmails: state.directEmails,
    roleIds: state.roleIds,
  };
  const server = {
    inheritConfig: state.serverData.inheritConfig,
    notificationDays: state.serverData.notificationDays || [],
    directEmails: state.serverData.directEmails || [],
    roleIds: state.serverData.roleIds || [],
  };
  state.isDirty = !equals(current, server);
  state.submitError = null;
};

const setInheritConfig = (state, { payload }) => {
  state.inheritConfig = payload;
  computeIsDirty(state);
};

const setNotificationDays = (state, { payload }) => {
  state.notificationDays = payload;
  computeIsDirty(state);
};

const setRecipientType = (state, { payload }) => {
  state.recipientType = payload;
  computeIsDirty(state);
};

const setDirectEmails = (state, { payload }) => {
  state.directEmails = payload;
  computeIsDirty(state);
};

const setRoleIds = (state, { payload }) => {
  state.roleIds = payload;
  computeIsDirty(state);
};

const loadRolesFulfilled = (state, { payload }) => {
  state.availableRoles = payload || [];
};

const waiverExpirationNotificationSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setInheritConfig,
    setNotificationDays,
    setRecipientType,
    setDirectEmails,
    setRoleIds,
    saveMaskTimerDone: (state) => {
      state.submitMaskState = null;
    },
  },
  extraReducers: {
    [loadConfig.pending]: loadConfigRequested,
    [loadConfig.fulfilled]: loadConfigFulfilled,
    [loadConfig.rejected]: loadConfigFailed,
    [saveConfig.pending]: saveConfigRequested,
    [saveConfig.fulfilled]: saveConfigFulfilled,
    [saveConfig.rejected]: saveConfigFailed,
    [loadRoles.fulfilled]: loadRolesFulfilled,
    [deleteConfig.pending]: (state) => {
      state.submitMaskState = false;
      state.submitError = null;
    },
    [deleteConfig.fulfilled]: (state) => {
      state.submitMaskState = null;
    },
    [deleteConfig.rejected]: (state, { payload }) => {
      state.submitMaskState = null;
      state.submitError = Messages.getHttpErrorMessage(payload);
    },
  },
});

export const actions = {
  ...waiverExpirationNotificationSlice.actions,
  loadConfig,
  loadRoles,
  goToEditor,
  saveConfig,
  deleteConfig,
};

export default waiverExpirationNotificationSlice.reducer;
