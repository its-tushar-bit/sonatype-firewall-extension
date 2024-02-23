/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { always, compose, flatten } from 'ramda';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getComponentWaivers, getProductFeaturesUrl, getReportPolicyThreatsUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { UI_ROUTER_ON_FINISH, stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { getAddWaiverPermissionForApplicationPromiseBuilder } from 'MainRoot/waivers/waiverActions';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { populateViolationsWithApplicableWaivers } from 'MainRoot/util/waiverUtils';
import { toggleBooleanProp } from 'MainRoot/util/reduxUtil';

const REDUCER_NAME = 'componentDetailsPolicyViolations';

export const initialState = {
  violations: null,
  waivers: null,
  loading: false,
  loadError: null,
  showComponentWaiversPopover: false,
  reloadComponentWaivers: false,
  showViolationsDetailPopover: false,
  violationsDetailRowClicked: false,
  hasPermissionToAddWaivers: false,
  innerSourceTransitiveWaiver: false,
  selectedPolicyViolationId: null,
  selectedPolicyViolation: null,
  violationType: null,
};

const loadRequested = (state) => {
  return {
    ...state,
    loading: true,
  };
};

const loadFulfilled = (state, { payload }) => {
  const {
    violationsResult = { aaData: [] },
    waiversResult = { waiversByOwner: [] },
    permissionResult,
    innerSourceTransitiveWaiver,
    hash,
  } = payload;

  const componentViolationInformation = violationsResult.aaData.find((violation) => violation.hash === hash) || {};
  const componentWaivers = flatten(
    waiversResult.waiversByOwner.map((waiversWithOwner) =>
      waiversWithOwner.waivers.map((waiver) => ({
        ...waiver,
        policyWaiverId: waiver.id,
        scopeOwnerId: waiversWithOwner.ownerId,
        scopeOwnerType: waiversWithOwner.ownerType,
        scopeOwnerName: waiversWithOwner.ownerName,
      }))
    )
  );

  const violations = componentViolationInformation.allViolations || componentViolationInformation.activeViolations;

  return {
    ...state,
    violations: populateViolationsWithApplicableWaivers(componentWaivers, violations),
    waivers: componentWaivers,
    loading: false,
    loadError: null,
    hasPermissionToAddWaivers: permissionResult,
    innerSourceTransitiveWaiver,
  };
};

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

function toggleComponentWaiversPopover(state) {
  const newVal = !state.showComponentWaiversPopover;
  return {
    ...state,
    showComponentWaiversPopover: newVal,
    reloadComponentWaivers: newVal,
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const { publicId, scanId, hash } = selectRouterCurrentParams(getState());
  const {
    application: { id },
  } = selectApplicationReportMetaData(getState());
  const applicationOwnerType = 'application';

  const promises = [
    axios.get(getReportPolicyThreatsUrl(publicId, scanId)),
    axios.get(getComponentWaivers(applicationOwnerType, publicId, hash)),
    getAddWaiverPermissionForApplicationPromiseBuilder(id),
    axios.get(getProductFeaturesUrl()),
  ];

  return Promise.all(promises)
    .then((results) => {
      const violationsResult = results[0].data;
      const waiversResult = results[1].data;
      const permissionResult = results[2].data.length === 1;
      const innerSourceTransitiveWaiver = results[3].data.includes('inner-source-transitive-waiver');
      return { violationsResult, waiversResult, permissionResult, innerSourceTransitiveWaiver, hash };
    })
    .catch(rejectWithValue);
});

const goToWaivers = (policyViolationId) => {
  return (dispatch, getState) => {
    const { hash } = selectRouterCurrentParams(getState());
    return dispatch(stateGo('applicationReport.violationWaivers', { hash, violationId: policyViolationId }));
  };
};

const componentDetailsViolationsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleComponentWaiversPopover,
    setSelectedPolicyViolation: propSet('selectedPolicyViolation'),
    setSelectedPolicyViolationId: propSet('selectedPolicyViolationId'),
    setViolationType: propSet('violationType'),
    toggleShowViolationsDetailPopover: toggleBooleanProp('showViolationsDetailPopover'),
    unsetShowViolationsDetailPopover: propSetConst('showViolationsDetailPopover', false),
    setViolationsDetailRowClicked: propSetConst('violationsDetailRowClicked', true),
    unsetViolationsDetailRowClicked: propSetConst('violationsDetailRowClicked', false),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [SELECT_COMPONENT]: always(initialState),
    [UI_ROUTER_ON_FINISH]: compose(
      propSetConst('showViolationsDetailPopover', false),
      propSetConst('violationsDetailRowClicked', false)
    ),
  },
});

export default componentDetailsViolationsSlice.reducer;
export const actions = {
  ...componentDetailsViolationsSlice.actions,
  load,
  goToWaivers,
};
