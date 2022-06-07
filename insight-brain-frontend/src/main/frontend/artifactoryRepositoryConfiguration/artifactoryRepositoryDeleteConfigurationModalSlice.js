/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, compose } from 'ramda';

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { getArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSetConst, propSet } from 'MainRoot/util/reduxToolkitUtil';

import { selectOwnerTypeAndOwnerId } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import { actions as baseConfigurationActions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import { selectArtifactoryConnectionId } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSelectors';

const REDUCER_NAME = 'artifactoryRepositoryDeleteConfigurationModal';

export const initialState = {
  artifactoryConnectionId: null,
  showModal: false,
  deleteSubmitMaskState: null,
  deleteConfigurationError: null,
};

function startDeleteSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.reset());
    dispatch(baseConfigurationActions.load());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const deleteConfiguration = createAsyncThunk(
  `${REDUCER_NAME}/deleteConfiguration`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState(),
      { ownerType, ownerId } = selectOwnerTypeAndOwnerId(state),
      artifactoryConnectionId = selectArtifactoryConnectionId(state);

    return axios
      .delete(getArtifactoryConnectionUrl(ownerType, ownerId, artifactoryConnectionId))
      .then(() => startDeleteSubmitMaskSuccessTimer(dispatch))
      .catch(rejectWithValue);
  }
);

function deleteConfigurationRequested(state) {
  return {
    ...state,
    deleteSubmitMaskState: false,
    deleteConfigurationError: null,
  };
}

function deleteConfigurationFailed(state, { payload }) {
  return {
    ...state,
    deleteSubmitMaskState: null,
    deleteConfigurationError: Messages.getHttpErrorMessage(payload),
  };
}

function deleteConfigurationFulfilled(state) {
  return {
    ...state,
    deleteSubmitMaskState: true,
  };
}

const artifactoryRepositoryDeleteConfigurationModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: always(initialState),
    openModal: compose(propSetConst('showModal', true), propSet('artifactoryConnectionId')),
    hideDeleteConfigurationModal: always(initialState),
  },
  extraReducers: {
    [deleteConfiguration.pending]: deleteConfigurationRequested,
    [deleteConfiguration.fulfilled]: deleteConfigurationFulfilled,
    [deleteConfiguration.rejected]: deleteConfigurationFailed,
  },
});

export default artifactoryRepositoryDeleteConfigurationModalSlice.reducer;

export const actions = {
  ...artifactoryRepositoryDeleteConfigurationModalSlice.actions,
  deleteConfiguration,
};
