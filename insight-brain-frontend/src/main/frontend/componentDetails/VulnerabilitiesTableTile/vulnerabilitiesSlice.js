/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { invertObj } from 'ramda';

import { pathSet } from 'MainRoot/util/jsUtil';
import {
  getVulnerabilitiesUrl,
  getVulnerabilityJsonDetailUrl,
  getVulnerabilityOverrideUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  selectSelectedVulnerability,
  selectVulnerabilitiesRequestData,
  selectVulnerabilityOverrideFormData,
  selectVulnerabityRefId,
} from './vulnerabilitiesSelectors';
import { validateMaxLength } from 'MainRoot/util/validationUtil';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;
const AVAILABLE_STATUS = {
  Open: 'OPEN',
  Acknowledged: 'ACKNOWLEDGED',
  'Not Applicable': 'NOT_APPLICABLE',
  Confirmed: 'CONFIRMED',
};

const REDUCER_NAME = 'componentDetailsVulnerabilities';

const initialState = {
  vulnerabilities: {
    data: null,
    loading: false,
    error: null,
  },
  showVulnerabilityDetailPopover: false,
  selectedRefId: null,
  vulnerabilityDetails: {
    loading: false,
    error: null,
    details: null,
  },
  vulnerabilitySecurityOverride: {
    status: '',
    comments: initUserInput(''),
    loading: false,
    loadError: null,
    submitMaskState: null,
    saveError: null,
  },
};

const loadVulnerabilities = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilities`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const urlData = selectVulnerabilitiesRequestData(state);
    const url = getVulnerabilitiesUrl(urlData);

    return axios
      .get(url)
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const loadVulnerabilityDetails = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilityDetails`,
  (_, { getState, rejectWithValue }) => {
    const refId = selectVulnerabityRefId(getState());
    return axios
      .get(getVulnerabilityJsonDetailUrl(refId))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

const saveVulnerabilityOverride = createAsyncThunk(
  `${REDUCER_NAME}/saveVulnerabilityOverride`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const { publicId: ownerId, hash } = selectRouterCurrentParams(getState());
    const { refId, source } = selectSelectedVulnerability(getState());
    const { status, comments } = selectVulnerabilityOverrideFormData(getState());

    const override = {
      status: status || 'OPEN',
      comment: comments.trimmedValue,
      referenceId: refId,
      hash,
      source,
    };
    return axios
      .put(getVulnerabilityOverrideUrl('application', ownerId), override)
      .then(({ data }) => {
        startSaveMaskSuccessTimer(dispatch);
        return data;
      })
      .catch(rejectWithValue);
  }
);
const startSaveMaskSuccessTimer = (dispatch) => {
  setTimeout(() => {
    dispatch(actions.saveVulnerabilityOverrideMaskDone());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const loadVulnerabilitiesFulfilled = (state, { payload }) => {
  state.vulnerabilities = {
    data: payload.data.securityVulnerabilities,
    loading: false,
    error: null,
  };
};

function loadVulnerabilitiesFailed(state, { payload }) {
  state.vulnerabilities.loading = false;
  state.vulnerabilities.error = Messages.getHttpErrorMessage(payload);
}

function loadVulnerabilityDetailsFulfilled(state, { payload }) {
  state.vulnerabilityDetails.loading = false;
  state.vulnerabilityDetails.error = null;
  state.vulnerabilityDetails.details = payload;

  const currentVulnerability = state.vulnerabilities.data.find(
    (vulnerability) => vulnerability.refId === state.selectedRefId
  );
  state.vulnerabilitySecurityOverride.loading = false;
  state.vulnerabilitySecurityOverride.loadError = null;
  state.vulnerabilitySecurityOverride.status = AVAILABLE_STATUS[currentVulnerability.status];
  state.vulnerabilitySecurityOverride.saveError = null;
}

function loadVulnerabilityDetailsFailed(state, { payload }) {
  state.vulnerabilityDetails.loading = false;
  state.vulnerabilityDetails.error = Messages.getHttpErrorMessage(payload);

  state.vulnerabilitySecurityOverride.loading = false;
  state.vulnerabilitySecurityOverride.loadError = Messages.getHttpErrorMessage(payload);
}

function toggleVulnerabilityPopoverWithEffects(state, { payload }) {
  state.selectedRefId = payload;
  state.showVulnerabilityDetailPopover = !state.showVulnerabilityDetailPopover;
  state.vulnerabilitySecurityOverride.status = '';
  state.vulnerabilitySecurityOverride.saveError = null;
  state.vulnerabilitySecurityOverride.comments = initUserInput('');
}

const componentDetailsVulnerabilitiesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleVulnerabilityPopoverWithEffects,
    setVulnerabilityOverrideStatus: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.status = payload;
      state.vulnerabilitySecurityOverride.comments = initUserInput('');
      state.vulnerabilitySecurityOverride.saveError = null;
    },
    setVulnerabilityOverrideComments: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.comments = userInput(validateMaxLength(1000), payload);
    },
    saveVulnerabilityOverrideMaskDone: (state) => {
      state.vulnerabilitySecurityOverride.submitMaskState = null;
    },
  },
  extraReducers: {
    [loadVulnerabilities.pending]: pathSet(['vulnerabilities', 'loading'], true),
    [loadVulnerabilities.fulfilled]: loadVulnerabilitiesFulfilled,
    [loadVulnerabilities.rejected]: loadVulnerabilitiesFailed,

    [loadVulnerabilityDetails.pending]: (state) => {
      state.vulnerabilityDetails.loading = true;
      state.vulnerabilitySecurityOverride.loading = true;
    },
    [loadVulnerabilityDetails.fulfilled]: loadVulnerabilityDetailsFulfilled,
    [loadVulnerabilityDetails.rejected]: loadVulnerabilityDetailsFailed,
    [saveVulnerabilityOverride.pending]: (state) => {
      state.vulnerabilitySecurityOverride.submitMaskState = false;
    },
    [saveVulnerabilityOverride.fulfilled]: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.submitMaskState = true;
      state.vulnerabilitySecurityOverride.comments = payload.comment
        ? userInput(() => {}, payload.comment)
        : state.vulnerabilitySecurityOverride.comments;
      state.vulnerabilitySecurityOverride.saveError = null;
      state.vulnerabilitySecurityOverride.comments.isPristine = true;

      const currentVulnerability = state.vulnerabilities.data.find(
        (vulnerability) => vulnerability.refId === state.selectedRefId
      );
      const fromServerStatus = invertObj(AVAILABLE_STATUS);
      currentVulnerability.status = fromServerStatus[payload.status ?? 'OPEN'];
    },
    [saveVulnerabilityOverride.rejected]: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.submitMaskState = null;
      state.vulnerabilitySecurityOverride.saveError = Messages.getHttpErrorMessage(payload);
    },
  },
});

export default componentDetailsVulnerabilitiesSlice.reducer;
export const actions = {
  ...componentDetailsVulnerabilitiesSlice.actions,
  loadVulnerabilities,
  loadVulnerabilityDetails,
  saveVulnerabilityOverride,
};
