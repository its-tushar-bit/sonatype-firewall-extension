/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, compose } from 'ramda';

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { propSetConst, propSet } from 'MainRoot/util/reduxToolkitUtil';

import { selectOwnerTypeAndOwnerId } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';
import { actions as baseConfigurationActions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import { selectRepositoryConnectionId } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSelectors';

const REDUCER_NAME = 'innerSourceRepositoryDeleteConfigurationModal';

export const initialState = {
  repositoryConnectionId: null,
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
      repositoryConnectionId = selectRepositoryConnectionId(state);

    return axios
      .delete(getRepositoryConnectionUrl(ownerType, ownerId, repositoryConnectionId))
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

const innerSourceRepositoryDeleteConfigurationModalSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: always(initialState),
    openModal: compose(propSetConst('showModal', true), propSet('repositoryConnectionId')),
    hideDeleteConfigurationModal: always(initialState),
  },
  extraReducers: {
    [deleteConfiguration.pending]: deleteConfigurationRequested,
    [deleteConfiguration.fulfilled]: deleteConfigurationFulfilled,
    [deleteConfiguration.rejected]: deleteConfigurationFailed,
  },
});

export default innerSourceRepositoryDeleteConfigurationModalSlice.reducer;

export const actions = {
  ...innerSourceRepositoryDeleteConfigurationModalSlice.actions,
  deleteConfiguration,
};
