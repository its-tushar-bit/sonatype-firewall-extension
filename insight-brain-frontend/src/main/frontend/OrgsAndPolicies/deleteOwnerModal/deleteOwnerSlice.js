/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { batch } from 'react-redux';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { getApplicationsUrl, getOrganizationsUrl, getRepositoryManagerById } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as ownerSideNavActions } from '../ownerSideNav/ownerSideNavSlice';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsApplication,
  selectIsOrganization,
  selectIsRepository,
  selectIsRepositoryManager,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

const REDUCER_NAME = `${OWNER_ACTIONS}/delete`;

export const initialState = {
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
};

const closeModal = (state) => {
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
};

const removeOwnerFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const removeOwnerFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const removeOwner = createAsyncThunk(`${REDUCER_NAME}/removeOwner`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const ownerToDelete = selectSelectedOwner(state);
  const isApp = selectIsApplication(state);
  const isOrg = selectIsOrganization(state);
  const isRepositoryManager = selectIsRepositoryManager(state);
  const isRepository = selectIsRepository(state);
  const isSbomManager = selectIsSbomManager(state);

  let url;
  if (isApp) {
    url = `${getApplicationsUrl()}/${ownerToDelete.publicId}`;
  } else if (isOrg) {
    url = `${getOrganizationsUrl()}/${ownerToDelete.id}`;
  } else if (isRepositoryManager) {
    url = getRepositoryManagerById(ownerToDelete.id);
  }

  return axios
    .delete(url)
    .then(() => {
      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
        batch(() => {
          if (isApp) {
            dispatch(
              stateGo(`${isSbomManager ? 'sbomManager.' : ''}management.view.organization`, {
                organizationId: ownerToDelete.organizationId,
              })
            );
            dispatch(ownerSideNavActions.removeApplicationFromOwnerHierarchy(ownerToDelete.publicId));
            dispatch(ownerSideNavActions.updateDisplayedOrganization({ organizationId: ownerToDelete.organizationId }));
          } else if (isOrg) {
            dispatch(
              stateGo(`${isSbomManager ? 'sbomManager.' : ''}management.view.organization`, {
                organizationId: ownerToDelete.parentOrganizationId,
              })
            );
            dispatch(ownerSideNavActions.removeOrganizationFromOwnerHierarchy(ownerToDelete.id));
            dispatch(
              ownerSideNavActions.updateDisplayedOrganization({ organizationId: ownerToDelete.parentOrganizationId })
            );
          } else if (isRepositoryManager) {
            dispatch(
              stateGo('management.view.repository_container', { repositoryContainerId: 'REPOSITORY_CONTAINER_ID' })
            );
            dispatch(ownerSideNavActions.removeRepositoryManagerFromOwnerHierarchy(ownerToDelete.id));
            dispatch(ownerSideNavActions.updateDisplayedOrganization({ organizationId: ownerToDelete.parentId }));
          } else if (isRepository) {
            // will be implemented when creating action menu for repository
          }
        });
      });
    })
    .catch((err) => rejectWithValue(err));
});

const deleteOwner = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
  },
  extraReducers: {
    [removeOwner.pending]: propSet('submitMaskState', false),
    [removeOwner.fulfilled]: removeOwnerFulfilled,
    [removeOwner.rejected]: removeOwnerFailed,
  },
});

export default deleteOwner.reducer;
export const actions = {
  ...deleteOwner.actions,
  removeOwner,
};
