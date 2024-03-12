/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { insert, propEq, find, not, equals, compose } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { getGlobalRoleMappingUrl } from 'MainRoot/utilAngular/CLMContextLocation';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { getFindUsersUrl, getRoleMappingUrl } from 'MainRoot/util/CLMLocation';
import { pathSet, propSet, propSetConst, pathSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import { selectAddedUsers, selectRoleToEdit } from './administratorsSelectors';
import { formatMembersForSaving, formatMembersForTransferList, removeFormatting } from 'MainRoot/util/formatGroupUsers';

const REDUCER_NAME = 'administratorsConfig';

const initialState = {
  serverData: null,
  loading: false,
  loadError: null,
  fetchUsers: { data: [], loading: false, loadError: null, partialError: null },
  rolesForCurrentOwner: { data: null, loading: false, loadError: null },
  addedUsers: [],
  serverAddedUsers: [],
  submitMaskState: null,
  submitError: null,
  isDirty: false,
};

function loadRequested(state) {
  state.loading = true;
}

function loadFulfilled(state, { payload }) {
  state.loading = false;
  state.loadError = null;
  state.serverData = payload;
}

function loadFailed(state, { payload }) {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
}

export const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { dispatch, getState, rejectWithValue }) => {
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => {
      return axios.get(getGlobalRoleMappingUrl()).then(({ data }) => {
        const roleId = selectRouterCurrentParams(getState()).roleId ?? '';
        const roleToEdit = find(propEq('roleId', roleId), data?.membersByRole ?? []);
        const addedUsers = roleToEdit?.membersByOwner?.[0]?.members ?? [];
        const formattedAddedUsers = formatMembersForTransferList(addedUsers);
        dispatch(actions.setServerAddedUsers(formattedAddedUsers));
        dispatch(actions.setAddedUsers(formattedAddedUsers));
        return data;
      });
    })
    .catch(rejectWithValue);
});

const loadFetchUsersFulfilled = (state, { payload }) => {
  state.fetchUsers.loading = false;
  state.fetchUsers.partialError = payload.error;
  state.fetchUsers.loadError = null;
  state.fetchUsers.data = formatMembersForTransferList(payload.members);
};

const loadFetchUsersFailed = (state, { payload }) => {
  state.fetchUsers.loading = false;
  state.fetchUsers.partialError = null;
  state.fetchUsers.loadError = Messages.getHttpErrorMessage(payload);
};

export const loadFetchUsers = createAsyncThunk(`${REDUCER_NAME}/loadFetchUsers`, (query, { rejectWithValue }) => {
  return axios
    .get(getFindUsersUrl(query))
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

const saveMembersRequested = (state) => {
  state.submitMaskState = false;
};

const saveMembersFulfilled = (state) => {
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveMembersFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

export const saveMembers = createAsyncThunk(
  `${REDUCER_NAME}/saveMembers`,
  (_, { rejectWithValue, getState, dispatch }) => {
    const state = getState();
    const { roleId } = selectRouterCurrentParams(state);
    const memberList = selectAddedUsers(state);
    const formatedMemberList = formatMembersForSaving(memberList);
    return axios
      .put(getRoleMappingUrl(roleId), formatedMemberList)
      .then(() => {
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(rejectWithValue);
  }
);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(actions.goToAdministrators());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export const loadRolesIfNeeded = () => (dispatch, getState) => {
  const state = getState();
  const roleToEdit = selectRoleToEdit(state);
  const roleToEditExistsInMemory = !!roleToEdit;
  dispatch(actions.clearErrors());

  if (!roleToEditExistsInMemory) {
    dispatch(actions.load());
  } else {
    //This ensures that Items Added in the NxSearchTransferList are up to date on initial load
    const addedUsers = roleToEdit?.membersByOwner?.[0]?.members ?? [];
    const formattedAddedUsers = formatMembersForTransferList(addedUsers);
    dispatch(actions.setServerAddedUsers(formattedAddedUsers));
    dispatch(actions.setAddedUsers(formattedAddedUsers));
  }
};

export const goToAdministratorPage = (roleId) => {
  return (dispatch) => {
    dispatch(stateGo('administratorsEdit', { roleId }));
  };
};

export const goToAdministrators = () => {
  return (dispatch) => {
    dispatch(stateGo('administrators'));
  };
};

const addSelectedUser = (state, { payload }) => {
  const formatedUsers = insert(0, removeFormatting(payload), state.addedUsers);
  return computeIsDirty({ ...state, addedUsers: formatedUsers });
};

const computeIsDirty = (state) => {
  const { addedUsers, serverAddedUsers } = state;
  const isDirty = not(equals(addedUsers, serverAddedUsers));
  return { ...state, isDirty };
};

const setAddedUsers = (state, { payload }) => {
  return computeIsDirty({ ...state, addedUsers: payload });
};

const administratorsConfigSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    addSelectedUser,
    setLoadingFetchUsers: pathSet(['fetchUsers', 'loading']),
    setAddedUsers,
    setServerAddedUsers: propSet('serverAddedUsers'),
    resetSubmitMaskState: propSetConst('submitMaskState', null),
    clearErrors: compose(
      pathSetConst(['fetchUsers', 'partialError'], null),
      pathSetConst(['fetchUsers', 'loadError'], null),
      propSetConst('submitError', null)
    ),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [loadFetchUsers.pending]: pathSetConst(['fetchUsers', 'loading'], true),
    [loadFetchUsers.fulfilled]: loadFetchUsersFulfilled,
    [loadFetchUsers.rejected]: loadFetchUsersFailed,
    [saveMembers.pending]: saveMembersRequested,
    [saveMembers.fulfilled]: saveMembersFulfilled,
    [saveMembers.rejected]: saveMembersFailed,
  },
});

export default administratorsConfigSlice.reducer;

export const actions = {
  ...administratorsConfigSlice.actions,
  load,
  loadFetchUsers,
  saveMembers,
  goToAdministratorPage,
  goToAdministrators,
  loadRolesIfNeeded,
};
