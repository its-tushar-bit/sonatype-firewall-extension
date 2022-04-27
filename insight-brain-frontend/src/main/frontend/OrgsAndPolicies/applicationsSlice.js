/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';

import { Messages } from 'MainRoot/util/CommonServices';
import { getApplicationsUrl, getMoveApplicationUrl } from '../util/CLMLocation';
import { isEmpty } from 'ramda';
import { selectEntityId } from './orgsAndPoliciesSelectors';
import { selectApplications } from './applicationsSelectors';
import { getOwnerName } from './utility/util';
import moveApplicationErrorMessages from 'MainRoot/owner.manager/move.application/move.application.messages';

import { propSet } from 'MainRoot/util/reduxToolkitUtil';
const REDUCER_NAME = 'applications';

export const initialState = {
  loadingApplications: false,
  loadApplicationsError: null,
  applications: [],
  ownerName: '',
};

const loadApplicationsRequested = (state) => {
  state.loadingApplications = true;
  state.loadApplicationsError = null;
};

const loadApplicationsFulfilled = (state, { payload }) => {
  state.loadingApplications = false;
  state.applications = payload;
};

const loadApplicationsFailed = (state, { payload }) => {
  state.loadingApplications = false;
  state.loadApplicationsError = Messages.getHttpErrorMessage(payload);
};

const loadApplications = createAsyncThunk(
  `${REDUCER_NAME}/loadApplications`,
  (_, { rejectWithValue, getState, dispatch }) => {
    return axios
      .get(getApplicationsUrl())
      .then((response) => {
        const entityId = selectEntityId(getState());
        const ownerName = getOwnerName(entityId)(response.data);
        dispatch(actions.setOwnerName(ownerName));
        return response.data;
      })
      .catch(rejectWithValue);
  }
);

const loadApplicationsIfNeeded = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicationsIfNeeded`,
  async (reload, { getState, dispatch }) => {
    const state = getState();
    let applications = selectApplications(state);

    if (isEmpty(applications) || reload) {
      applications = unwrapResult(await dispatch(actions.loadApplications()));
    } else {
      const entityId = selectEntityId(getState());
      const ownerName = getOwnerName(entityId)(applications);
      dispatch(actions.setOwnerName(ownerName));
    }

    return Promise.resolve(applications);
  }
);

const moveApplication = ({ applicationId, organizationId }) => {
  return (dispatch) => {
    return axios
      .post(getMoveApplicationUrl(applicationId, organizationId))
      .then((response) => {
        return dispatch(actions.loadApplications()).then(() => {
          return response?.data?.warnings;
        });
      })
      .catch((error) => {
        if (error.response.status === 409 && error.response.data?.errors?.length) {
          // data.errors is an array of incompatibilities
          return Promise.reject({
            message: moveApplicationErrorMessages.ERROR_INCOMPATIBLE_DESTINATION,
            incompatibilities: error.response.data.errors,
          });
        }
        return Promise.reject(error);
      });
  };
};

const applicationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setOwnerName: propSet('ownerName'),
  },
  extraReducers: {
    [loadApplications.pending]: loadApplicationsRequested,
    [loadApplications.fulfilled]: loadApplicationsFulfilled,
    [loadApplications.rejected]: loadApplicationsFailed,
  },
});

export const actions = {
  ...applicationsSlice.actions,
  loadApplications,
  loadApplicationsIfNeeded,
  moveApplication,
};

export default applicationsSlice.reducer;
