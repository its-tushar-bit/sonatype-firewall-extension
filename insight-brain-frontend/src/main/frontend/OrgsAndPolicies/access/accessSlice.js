/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import {
  getAccessPageRolesUrl,
  getCreateOrDeleteAccessUrl,
  getUsersRoleMappingUrl,
  getCreateOrDeleteAccessRepositoryUrl,
  getRepositoryContainerRoleMappingUrl,
  getUsersRepositoryRoleMappingUrl,
} from 'MainRoot/util/CLMLocation';
import {
  selectRouterSlice,
  selectRouterCurrentParams,
  selectIsRepositoriesRelated,
  selectIsRepositoryContainer,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { map, prepend, isEmpty, symmetricDifference, includes, pick, groupBy, reject } from 'ramda';
import {
  formatGroupUsers,
  formatMembersForSaving,
  formatMembersForTransferList,
  removeFormatting,
} from 'MainRoot/util/formatGroupUsers';
import { pathSet } from 'MainRoot/util/reduxToolkitUtil';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { selectUnSortedAddedUsers, selectRole, selectRoleToEdit } from './accessSelectors';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'access';

export const initialState = {
  addedUsers: [],
  inheritedAccessOpen: null,
  serverAddedUsers: [],
  siblings: [],
  fetchUsers: { data: [], loading: false, loadError: null, partialError: null, mostRecentUserQuery: null },
  availableRoles: null,
  loadError: null,
  submitError: null,
  loading: false,
  isDirty: false,
  role: null,
  isNew: false,
  deleteMaskState: null,
  submitMaskState: null,
  deleteError: null,
  isRepositories: false,
  serverData: null,
  groupName: rscInitialState(''),
  noRolesAvailableError: null,
  fetchUsersData: [],
};

const toggleInheritedAccessOpen = (state, { payload }) => {
  state.inheritedAccessOpen[payload] = !state.inheritedAccessOpen[payload];
};

export const loadRolesIfNeeded = () => (dispatch, getState) => {
  const state = getState();
  const roleToEdit = selectRoleToEdit(state);
  const roleToEditExistsInMemory = !!roleToEdit;
  dispatch(actions.clearDeleteError());
  if (!roleToEditExistsInMemory) {
    dispatch(actions.loadRoles());
  } else {
    const addedUsers = roleToEdit?.membersByOwner?.[0]?.members ?? [];
    const formattedAddedUsers = formatMembersForTransferList(addedUsers);
    dispatch(actions.setServerAddedUsers(formattedAddedUsers));
    dispatch(actions.setAddedUsers(formattedAddedUsers));
  }
};

export const loadRoles = createAsyncThunk(`${REDUCER_NAME}/loadRoles`, (_, { getState, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const { roleId = '' } = selectRouterCurrentParams(state);
  const isRepositoryContainer = selectIsRepositoryContainer(state);

  return axios
    .get(isRepositoryContainer ? getRepositoryContainerRoleMappingUrl() : getAccessPageRolesUrl(ownerType, ownerId))
    .then(({ data }) => {
      return { data, roleId };
    })
    .catch(rejectWithValue);
});

const loadRolesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadRolesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.isNew = !payload.roleId;
  state.isDirty = false;
  state.serverData = payload.data;
  state.groupName = rscInitialState('');

  if (state.isNew) {
    state.role = null;
    state.addedUsers = [];
    state.serverAddedUsers = [];
  } else {
    const role = payload.data.membersByRole.find((role) => payload.roleId === role.roleId);
    state.role = pick(['roleId', 'roleName', 'roleDescription'], role);
    const formattedMembers = compose(formatGroupUsers, formatMembersForTransferList)(role.membersByOwner[0].members);
    state.addedUsers = formattedMembers;
    state.serverAddedUsers = formattedMembers;
    if (!state.role) {
      state.loadError = 'Could not find a role with ID: ' + payload.roleId + '.';
      return;
    }
  }
  const { withoutLocalMembers = [], withLocalMembers = [] } = groupRolesByMembership(payload.data.membersByRole);
  state.availableRoles = withoutLocalMembers;
  state.siblings = withLocalMembers;

  const newInheritedAccessOpen = {};
  payload.data.membersByRole?.forEach((role) => {
    role?.membersByOwner?.forEach((member) => {
      newInheritedAccessOpen[member.ownerId] = true;
    });
  });
  state.inheritedAccessOpen = newInheritedAccessOpen;
};

const loadRolesFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

export const loadFetchUsers = createAsyncThunk(
  `${REDUCER_NAME}/loadFetchUsers`,
  (query, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    const isRepositoriesRelated = selectIsRepositoriesRelated(getState());
    return axios
      .get(
        isRepositoriesRelated
          ? getUsersRepositoryRoleMappingUrl(query)
          : getUsersRoleMappingUrl(ownerType, ownerId, query)
      )
      .then(({ data }) => {
        return { ...data, query };
      })
      .catch(rejectWithValue);
  }
);

const loadFetchUsersRequested = (state, { meta }) => {
  state.fetchUsers.loading = true;
  state.fetchUsers.mostRecentUserQuery = meta.arg;
};

const loadFetchUsersFulfilled = (state, { payload }) => {
  if (payload.query === state.fetchUsers.mostRecentUserQuery) {
    state.fetchUsers.loading = false;
    state.fetchUsers.partialError = payload.error;
    state.fetchUsers.loadError = null;
    const addedUsers = state.addedUsers;
    const leftedUsers = compose(
      reject((user) => includes(user, addedUsers)),
      formatGroupUsers
    )(formatMembersForTransferList(payload.members));
    state.fetchUsers.data = formatMembersForTransferList(leftedUsers);
  }
};

const loadFetchUsersFailed = (state, { payload }) => {
  state.fetchUsers.loading = false;
  state.fetchUsers.partialError = null;
  state.fetchUsers.loadError = Messages.getHttpErrorMessage(payload);
};

const addSelectedUser = (state, { payload }) => {
  const formattedUsers = prepend(payload, state.addedUsers);
  return computeIsDirty({ ...state, addedUsers: formattedUsers });
};

const addSelectedUserGroup = (state) => {
  const {
    groupName: { trimmedValue },
    addedUsers,
  } = state;
  const group = {
    displayName: `${trimmedValue} (Group)`,
    email: null,
    id: `${trimmedValue}GROUP`,
    internalName: trimmedValue,
    realm: null,
    type: 'GROUP',
  };

  const formattedUsers = prepend(group, addedUsers);
  return computeIsDirty({ ...state, addedUsers: formattedUsers, groupName: rscInitialState('') });
};

const computeIsDirty = (state) => {
  const { addedUsers, serverAddedUsers, role, isNew } = state;
  const equalIgnoreOrder = compose(isEmpty, symmetricDifference);
  const isDirty = isNew ? !!role || addedUsers.length > 0 : !equalIgnoreOrder(addedUsers, serverAddedUsers);
  return { ...state, isDirty };
};

const setAddedUsers = (state, { payload }) => {
  return computeIsDirty({ ...state, addedUsers: payload });
};

const setGroupName = (state, { payload }) => {
  return computeIsDirty({ ...state, groupName: userInput(null, payload) });
};

export const removeRole = createAsyncThunk(
  `${REDUCER_NAME}/removeRole`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const roleId = selectRouterCurrentParams(state).roleId ?? '';
    const isRepositoryContainer = selectIsRepositoryContainer(state);
    return axios
      .put(
        isRepositoryContainer
          ? getCreateOrDeleteAccessRepositoryUrl(roleId)
          : getCreateOrDeleteAccessUrl(ownerType, ownerId, roleId),
        []
      )
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone).then(() => dispatch(goToAddAccess()));
        return roleId;
      })
      .catch(rejectWithValue);
  }
);

export const createOrUpdateRole = createAsyncThunk(
  `${REDUCER_NAME}/createOrUpdateRole`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const role = selectRole(state);
    const memberList = selectUnSortedAddedUsers(state);
    const formatedMemberList = compose(map(removeFormatting), formatMembersForSaving)(memberList);
    const isRepositoryContainer = selectIsRepositoryContainer(state);

    return axios
      .put(
        isRepositoryContainer
          ? getCreateOrDeleteAccessRepositoryUrl(role?.roleId)
          : getCreateOrDeleteAccessUrl(ownerType, ownerId, role?.roleId),
        formatedMemberList
      )
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
      })
      .catch(rejectWithValue);
  }
);

const createOrUpdateRoleRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const createOrUpdateRoleFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
  state.loading = false;
  state.isDirty = false;
  state.siblings?.push(state.role);
  state.availableRoles = state.availableRoles?.filter((role) => role.roleId !== state.role?.roleId);
  if (state.isNew) {
    state.addedUsers = [];
    state.role = null;
  }
  state.serverAddedUsers = state.addedUsers;
  state.fetchUsers = { data: [], loading: false, loadError: null, partialError: null };
};

const createOrUpdateRoleFailed = (state, { payload }) => {
  state.loading = false;
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const removeRoleRequested = (state) => {
  state.loading = true;
  state.loadError = null;
  state.isDirty = false;
  state.deleteMaskState = null;
  state.deleteError = null;
};

const removeRoleFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.deleteError = null;
  state.siblings = state.siblings.filter((role) => role.roleId === payload);
  state.isDirty = false;
  state.deleteMaskState = true;
};

const removeRoleFailed = (state, { payload }) => {
  state.loading = false;
  state.deleteError = Messages.getHttpErrorMessage(payload);
  state.deleteMaskState = null;
};

export const goToAddAccess = createAsyncThunk(`${REDUCER_NAME}/goToAddAccess`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'add-access');
  dispatch(stateGo(to, params));
});

const setRole = (state, { payload }) => {
  const { availableRoles } = state;
  const selectedRole = availableRoles.find((role) => role.roleId === payload);
  return computeIsDirty({ ...state, role: selectedRole });
};

const clearDeleteError = (state) => {
  state.deleteError = null;
};

const accessEditPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    addSelectedUser,
    addSelectedUserGroup,
    setAddedUsers,
    setRole,
    setGroupName,
    setServerAddedUsers: propSet('serverAddedUsers'),
    setLoadingFetchUsers: pathSet(['fetchUsers', 'loading']),
    saveMaskTimerDone: propSet('submitMaskState', null),
    deleteMaskTimerDone: propSet('deleteMaskState', null),
    clearDeleteError,
    toggleInheritedAccessOpen,
  },
  extraReducers: {
    [loadRoles.pending]: loadRolesRequested,
    [loadRoles.fulfilled]: loadRolesFulfilled,
    [loadRoles.rejected]: loadRolesFailed,
    [createOrUpdateRole.pending]: createOrUpdateRoleRequested,
    [createOrUpdateRole.fulfilled]: createOrUpdateRoleFulfilled,
    [createOrUpdateRole.rejected]: createOrUpdateRoleFailed,
    [removeRole.pending]: removeRoleRequested,
    [removeRole.fulfilled]: removeRoleFulfilled,
    [removeRole.rejected]: removeRoleFailed,
    [loadFetchUsers.pending]: loadFetchUsersRequested,
    [loadFetchUsers.fulfilled]: loadFetchUsersFulfilled,
    [loadFetchUsers.rejected]: loadFetchUsersFailed,
  },
});

const groupRolesByMembership = (membersByRole) => {
  if (membersByRole) {
    return groupBy(
      ({ membersByOwner: [{ members }] }) => `with${isEmpty(members) ? 'out' : ''}LocalMembers`,
      membersByRole
    );
  } else {
    return { withLocalMembers: [], withoutLocalMembers: [] };
  }
};

export default accessEditPageSlice.reducer;
export const actions = {
  ...accessEditPageSlice.actions,
  loadRoles,
  loadFetchUsers,
  createOrUpdateRole,
  selectRole,
  removeRole,
  loadRolesIfNeeded,
  goToAddAccess,
};
