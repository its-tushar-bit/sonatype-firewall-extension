/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, invertObj } from 'ramda';

import { pathSet } from 'MainRoot/util/jsUtil';
import {
  getApplicationSummaryUrl,
  getVulnerabilitiesUrl,
  getVulnerabilityJsonDetailUrl,
  getVulnerabilityOverrideUrl,
} from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectSelectedVulnerability,
  selectVulnerabilitiesRequestData,
  selectFirewallVulnerabilitiesRequestData,
  selectVulnerabilityOverrideFormData,
} from './vulnerabilitiesSelectors';
import { selectRouteParamsFromSecurityTab, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { validateMaxLength } from 'MainRoot/util/validationUtil';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;
const AVAILABLE_STATUS = {
  Open: 'OPEN',
  Acknowledged: 'ACKNOWLEDGED',
  'Not Applicable': 'NOT_APPLICABLE',
  Confirmed: 'CONFIRMED',
};

const REDUCER_NAME = 'componentDetailsVulnerabilities';

export const initialState = {
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
  hasEditIqPermission: false,
  vulnerabilitySecurityOverride: {
    status: '',
    comments: initUserInput(''),
    showCommentField: true,
    loading: false,
    loadError: null,
    submitMaskState: null,
    saveError: null,
    isDisabled: true,
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

const loadFirewallVulnerabilities = createAsyncThunk(
  `${REDUCER_NAME}/loadVulnerabilities`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const urlData = selectFirewallVulnerabilitiesRequestData(state);
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
    const component = selectSelectedComponent(getState());
    const componentIdentifier = component?.componentIdentifier;
    const identificationSource = component?.identificationSource;
    const scanId = selectRouterCurrentParams(getState())?.scanId;
    const { ownerId, hash, isRepositoryComponent } = selectRouteParamsFromSecurityTab(getState());
    const ownerType = isRepositoryComponent ? 'repository' : 'application';
    const vulnerability = selectSelectedVulnerability(getState());
    const extraQueryParameters = {
      ownerType: ownerType,
      ownerId: ownerId,
      identificationSource: identificationSource,
      scanId: scanId,
    };
    const vulnerabilityJsonDetailUrl = getVulnerabilityJsonDetailUrl(
      vulnerability.refId,
      componentIdentifier,
      extraQueryParameters
    );
    const vulnerabilityOverrideUrl = getVulnerabilityOverrideUrl(ownerType, ownerId, hash, vulnerability);

    return axios
      .all([axios.get(vulnerabilityJsonDetailUrl), axios.get(vulnerabilityOverrideUrl)])
      .then(([{ data: vulnerabilityDetails }, { data: vulnerabilityOverride }]) => {
        if (ownerType === 'application') {
          return axios
            .get(getApplicationSummaryUrl(ownerId))
            .then(({ data }) => {
              return checkPermissions(['WRITE'], ownerType, data.id);
            })
            .then(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment, hasEditIqPermission: true };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        } else {
          return checkPermissions(['WRITE'], 'repository', ownerId)
            .then((_) => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment, hasEditIqPermission: true, _ };
            })
            .catch(() => {
              return { ...vulnerabilityDetails, comment: vulnerabilityOverride.comment };
            });
        }
      })
      .catch(rejectWithValue);
  }
);

const saveVulnerabilityOverride = createAsyncThunk(
  `${REDUCER_NAME}/saveVulnerabilityOverride`,
  (_, { dispatch, getState, rejectWithValue }) => {
    const { ownerId, hash, isRepositoryComponent } = selectRouteParamsFromSecurityTab(getState());
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
      .put(getVulnerabilityOverrideUrl(isRepositoryComponent ? 'repository' : 'application', ownerId), override)
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
  state.hasEditIqPermission = payload?.hasEditIqPermission;

  state.vulnerabilitySecurityOverride.comments = initUserInput(payload?.comment || '');

  const currentVulnerability = state.vulnerabilities.data.find(
    (vulnerability) => vulnerability.refId === state.selectedRefId
  );
  state.vulnerabilitySecurityOverride.loading = false;
  state.vulnerabilitySecurityOverride.loadError = null;
  state.vulnerabilitySecurityOverride.saveError = null;

  const status = AVAILABLE_STATUS[currentVulnerability.status];
  state.vulnerabilitySecurityOverride.status = status;
  if (status === 'OPEN') {
    state.vulnerabilitySecurityOverride.showCommentField = false;
  }
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
  state.vulnerabilitySecurityOverride.showCommentField = true;
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
      state.vulnerabilitySecurityOverride.showCommentField = true;
    },
    setVulnerabilityOverrideComments: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.comments = userInput(validateMaxLength(1000), payload);
    },
    saveVulnerabilityOverrideMaskDone: (state) => {
      state.vulnerabilitySecurityOverride.submitMaskState = null;
    },
    setVulnerabilityOverrideFormDisabled: (state, { payload }) => {
      state.vulnerabilitySecurityOverride.isDisabled = payload;
    },
  },
  extraReducers: {
    [loadVulnerabilities.pending]: pathSet(['vulnerabilities', 'loading'], true),
    [loadVulnerabilities.fulfilled]: loadVulnerabilitiesFulfilled,
    [loadVulnerabilities.rejected]: loadVulnerabilitiesFailed,

    [loadFirewallVulnerabilities.pending]: pathSet(['vulnerabilities', 'loading'], true),
    [loadFirewallVulnerabilities.fulfilled]: loadVulnerabilitiesFulfilled,
    [loadFirewallVulnerabilities.rejected]: loadVulnerabilitiesFailed,

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
    [SELECT_COMPONENT]: always(initialState),
  },
});

export default componentDetailsVulnerabilitiesSlice.reducer;
export const actions = {
  ...componentDetailsVulnerabilitiesSlice.actions,
  loadVulnerabilities,
  loadFirewallVulnerabilities,
  loadVulnerabilityDetails,
  saveVulnerabilityOverride,
};
