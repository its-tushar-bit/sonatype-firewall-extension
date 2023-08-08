/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { debounce } from 'debounce';
import { any, always, propEq } from 'ramda';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { NX_STANDARD_DEBOUNCE_TIME } from '@sonatype/react-shared-components';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { formatMembersForTransferList } from 'MainRoot/util/formatGroupUsers';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { getUsersRoleMappingUrl, getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { selectOwnerProperties, selectSelectedOwner, selectSelectedOwnerContact } from '../orgsAndPoliciesSelectors';
import { selectContactSlice } from './selectContactModalSelectors';

const REDUCER_NAME = `${OWNER_ACTIONS}/selectContact`;

export const initialState = {
  selectedContact: null,
  fetchedUsers: {
    // The query which was used to fetch the current data, used to prevent spurious re-fetches
    query: null,
    data: [],
    loading: false,
    loadError: null,
    partialError: null,
  },

  // current value of the combobox text box
  query: '',
  loadError: null,
  submitError: null,
  isDirty: false,
  isValid: true,
  submitMaskState: null,
  isContactModalOpen: false,
  showUnsavedChangesModal: false,

  // duplicate of orgsAndPolicies.root.selectedOwner.contact
  savedContact: null,
};

// Note that this cannot be triggered directly by an externally-visible action. Instead the externally-visible
// `openContactModal` action is actually the `openContactModalAction` defined below.
const openContactModal = (state, { payload: savedContact }) => {
  state.isContactModalOpen = true;
  if (!isNilOrEmpty(savedContact)) {
    state.savedContact = state.selectedContact = savedContact;
    state.query = savedContact.displayName;
    state.fetchedUsers.data = formatMembersForTransferList([savedContact]);
  }
};

// getState is not available in reducers so this custom action calls it to retrieve the selected owner contact from
// another part of the store
const openContactModalAction = () => (dispatch, getState) => {
  dispatch({
    type: `${REDUCER_NAME}/openContactModal`,
    payload: selectSelectedOwnerContact(getState()),
  });
};

const closeContactModal = always(initialState);
const cancelContactModal = (state) => {
  if (state.isDirty) {
    state.showUnsavedChangesModal = true;
  } else {
    return closeContactModal();
  }
};

const closeUnsavedChangesModal = (state) => {
  state.showUnsavedChangesModal = false;
};

export const setQuery = (state, { payload: query }) => {
  state.query = query;
  if (query === '') {
    clearMatches(state);
  }
  updateComputedProperties(state);
};

const computeIsDirty = (state) => {
  const { savedContact, selectedContact, query } = state;

  if (savedContact) {
    // if there is a saved contact, the form is dirty if a different contact (or no contact) is selected
    return savedContact.internalName !== selectedContact?.internalName;
  } else {
    // if there is no saved contact, then any currently selected contact or input in the combobox makes the form dirty
    return !!(selectedContact || query);
  }
};

const computeIsValid = (state) => {
  // valid if a contact is selected or the query box is empty (which signifies a "no contact" selection)
  return !!state.selectedContact || !state.query;
};

const updateComputedProperties = (state) => {
  state.isDirty = computeIsDirty(state);
  state.isValid = computeIsValid(state);
};

export const clearMatches = (state) => {
  state.fetchedUsers = initialState.fetchedUsers;
  state.selectedContact = null;

  updateComputedProperties(state);
};

export const setSelectedContact = (state, { payload: selectedContact }) => {
  state.selectedContact = selectedContact ?? null;
  updateComputedProperties(state);
};

export const loadFetchUsers = createAsyncThunk(
  `${REDUCER_NAME}/loadFetchUsers`,
  (query, { getState, rejectWithValue }) => {
    if (query === '') {
      return { members: [], query: '' };
    } else {
      const { ownerType, ownerId } = selectOwnerProperties(getState());

      return axios
        .get(getUsersRoleMappingUrl(ownerType, ownerId, query, false))
        .then(({ data }) => {
          return { ...data, query };
        })
        .catch(rejectWithValue);
    }
  }
);

const debouncedLoadFetchUsers = debounce((dispatch, query) => {
  dispatch(actions.loadFetchUsers(query));
}, NX_STANDARD_DEBOUNCE_TIME);

const setFetchUsersLoading = (state) => {
  state.fetchedUsers.loading = true;
};

const loadFetchUsersPending = (state, action) => {
  state.fetchedUsers.query = action.meta.arg;
};

const loadFetchUsersFulfilled = (state, { payload }) => {
  // Guard against out-of-order fetch responses by only processing results of the latest query
  if (payload.query === state.fetchedUsers.query) {
    state.fetchedUsers.data = formatMembersForTransferList(payload.members);
    state.fetchedUsers.partialError = payload.error;
    state.fetchedUsers.loading = false;
    state.fetchedUsers.loadError = null;

    const newDataContainsSelectedContact =
      state.selectedContact && any(propEq('internalName', state.selectedContact.internalName), state.fetchedUsers.data);

    if (!newDataContainsSelectedContact) {
      state.selectedContact = null;
    }

    updateComputedProperties(state);
  }
};

const loadFetchUsersFailed = (state, { payload }) => {
  state.fetchedUsers.loading = false;
  state.fetchedUsers.partialError = null;
  state.fetchedUsers.loadError = Messages.getHttpErrorMessage(payload);
};

const prepareQuery = (query) => {
  const normalizedWhitespaceQuery = query.trim().replace(/\s{2,}/g, ' ');

  if (normalizedWhitespaceQuery === '') {
    return '';
  } else {
    return normalizedWhitespaceQuery.includes('*') ? normalizedWhitespaceQuery : `${normalizedWhitespaceQuery}*`;
  }
};

const searchUsers = (query) => (dispatch, getState) => {
  const preparedQuery = prepareQuery(query);

  if (preparedQuery !== selectContactSlice(getState()).fetchedUsers.query) {
    // loading flag must be set before debounce to prevent blip of empty message
    dispatch(actions.setFetchUsersLoading());
    debouncedLoadFetchUsers(dispatch, preparedQuery);
  }
};

export const saveContact = createAsyncThunk(
  `${REDUCER_NAME}/saveContact`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { id, name, organizationId, publicId } = selectSelectedOwner(state);
    const { selectedContact } = selectContactSlice(state);
    const contactInternalName = selectedContact?.internalName;
    const submitData = {
      contactInternalName,
      id,
      name,
      organizationId,
      publicId,
    };
    return axios
      .put(getApplicationsUrl(), submitData)
      .then(({ data }) => {
        dispatch(rootActions.setSelectedOwnerContact(data.contact));
        startSaveMaskSuccessTimer(dispatch, actions.closeContactModal);
      })
      .catch(rejectWithValue);
  }
);

const saveContactRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveContactFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
  state.isDirty = false;
};

const saveContactFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const contact = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openContactModal,
    closeContactModal,
    cancelContactModal,
    closeUnsavedChangesModal,
    setQuery,
    clearMatches,
    setSelectedContact,
    setFetchUsersLoading,
    saveMaskTimerDone: propSet('submitMaskState', null),
  },
  extraReducers: {
    [saveContact.pending]: saveContactRequested,
    [saveContact.fulfilled]: saveContactFulfilled,
    [saveContact.rejected]: saveContactFailed,
    [loadFetchUsers.pending]: loadFetchUsersPending,
    [loadFetchUsers.fulfilled]: loadFetchUsersFulfilled,
    [loadFetchUsers.rejected]: loadFetchUsersFailed,
  },
});

export default contact.reducer;
export const actions = {
  ...contact.actions,
  loadFetchUsers,
  saveContact,
  searchUsers,
  openContactModal: openContactModalAction,
};
