/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { checkPermissions } from '../../../util/authorizationUtil';
import axios from 'axios';
import { getLdapConfigUrl, getLdapPriority } from '../../../util/CLMLocation';
import { propSetConst } from '../../../util/reduxToolkitUtil';
import { Messages } from '../../../utilAngular/CommonServices';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

const REDUCER_NAME = 'ldapList';

export const initialState = {
  servers: [],
  loading: false,
  loadError: null,
  reorderedServers: null,
  saveServerOrderSuccess: null,
  saveServerOrderError: null,
  isDirty: false,
};

function loadFulfilled(state, { payload }) {
  return {
    ...state,
    loading: false,
    servers: payload,
    reorderedServers: null,
  };
}

function loadFailed(state, { payload }) {
  return {
    ...state,
    servers: [],
    loadError: Messages.getHttpErrorMessage(payload),
    loading: false,
  };
}

const loadServers = createAsyncThunk(`${REDUCER_NAME}/loadServers`, (skipPermissionsCheck, { rejectWithValue }) => {
  const permissionsCheckPromise = skipPermissionsCheck ? Promise.resolve() : checkPermissions(['CONFIGURE_SYSTEM']);
  return permissionsCheckPromise
    .then(() => axios.get(getLdapConfigUrl()))
    .then(({ data }) => data)
    .catch(rejectWithValue);
});

const saveOrder = createAsyncThunk(`${REDUCER_NAME}/saveOrder`, (_, { rejectWithValue, getState, dispatch }) => {
  const serverIds = getState()[REDUCER_NAME].reorderedServers.map(prop('id'));
  return checkPermissions(['CONFIGURE_SYSTEM'])
    .then(() => axios.put(getLdapPriority(), serverIds))
    .then(() => startSaveMaskAndReloadTimer(dispatch))
    .catch(rejectWithValue);
});

function startSaveMaskAndReloadTimer(dispatch) {
  setTimeout(() => {
    dispatch(actions.saveMaskTimerDone());
    dispatch(loadServers(true));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const loadListSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    enterReorderMode(state) {
      state.reorderedServers = [...state.servers];
      state.isDirty = false;
    },
    exitReorderMode(state) {
      state.reorderedServers = null;
      state.saveServerOrderError = null;
      state.isDirty = false;
    },
    moveServerUpInTheList(state, action) {
      const index = action.payload;
      if (index === 0 || index >= state.reorderedServers.length) {
        return state;
      }
      const newIndex = index - 1;
      const server = state.reorderedServers[index];
      state.reorderedServers[index] = state.reorderedServers[newIndex];
      state.reorderedServers[newIndex] = server;
      state.isDirty = hasOrderChanged(state);
    },
    saveMaskTimerDone: propSetConst('saveServerOrderSuccess', null),
  },
  extraReducers: {
    [loadServers.pending]: propSetConst('loading', true),
    [loadServers.fulfilled]: loadFulfilled,
    [loadServers.rejected]: loadFailed,
    [saveOrder.pending]: propSetConst('saveServerOrderSuccess', false),
    [saveOrder.fulfilled](state) {
      state.saveServerOrderSuccess = true;
      state.saveServerOrderError = null;
      state.isDirty = false;
    },
    [saveOrder.rejected](state, { payload }) {
      state.saveServerOrderSuccess = null;
      state.saveServerOrderError = Messages.getHttpErrorMessage(payload);
    },
  },
});

function hasOrderChanged({ servers, reorderedServers }) {
  const firstServerOutOfOrder = reorderedServers.find((server, index) => server.id !== servers[index].id);
  return firstServerOutOfOrder != null;
}

export const actions = {
  ...loadListSlice.actions,
  loadServers,
  saveOrder,
};

export default loadListSlice.reducer;
