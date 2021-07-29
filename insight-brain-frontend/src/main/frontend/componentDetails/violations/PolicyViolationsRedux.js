/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { equals, flatten } from 'ramda';

import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';
import { getComponentWaivers, getReportPolicyThreatsUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { propSet } from '../../util/reduxToolkitUtil';

const REDUCER_NAME = 'componentDetailsPolicyViolations';

const initialState = {
  violations: null,
  waivers: null,
  loading: false,
  loadError: null,
  showViolationsDetail: false,
};

const loadRequested = (state) => {
  return {
    ...state,
    loading: true,
  };
};

const loadFulfilled = (state, { payload }) => {
  const { violationsResult = { aaData: [] }, waiversResult = { waiversByOwner: [] }, hash } = payload;

  const componentViolationInformation = violationsResult.aaData.find((violation) => violation.hash === hash);
  const componentWaivers = flatten(
    waiversResult.waiversByOwner.map((waiversWithOwner) =>
      waiversWithOwner.waivers.map((waiver) => ({
        ...waiver,
        type: waiversWithOwner.ownerType,
        ownerName: waiversWithOwner.ownerName,
      }))
    )
  );

  return {
    ...state,
    violations: mapWaiversInformationToViolations(componentWaivers, componentViolationInformation.allViolations),
    waivers: componentWaivers,
    loading: false,
    loadError: null,
  };
};

const mapWaiversInformationToViolations = (componentWaivers, allViolations) => {
  // the waivers are already filtered for the component so there's no need for a hash matcher
  const matchesPolicyId = (waiver, violation) => waiver.policyId === violation.policyId;
  const matchesConstraintFacts = (waiver, violation) =>
    waiver.constraintFactsJson != null && equals(waiver.constraintFactsJson, violation.constraintFactsJson);

  const waiverIsApplicableToViolation = (waiver, violation) => {
    return matchesPolicyId(waiver, violation) && matchesConstraintFacts(waiver, violation);
  };

  return allViolations.map((violation) => ({
    ...violation,
    applicableWaivers: componentWaivers
      .filter((waiver) => waiverIsApplicableToViolation(waiver, violation))
      .map((waiver) => waiver.id),
  }));
};

function loadFailed(state, { payload }) {
  return {
    ...state,
    loading: false,
    loadError: Messages.getHttpErrorMessage(payload),
  };
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const { publicId, scanId, hash } = selectRouterCurrentParams(getState());
  const applicationOwnerType = 'application';

  const promises = [
    axios.get(getReportPolicyThreatsUrl(publicId, scanId)),
    axios.get(getComponentWaivers(applicationOwnerType, publicId, hash)),
  ];

  return Promise.all(promises)
    .then((results) => {
      const violationsResult = results[0].data;
      const waiversResult = results[1].data;

      return { violationsResult, waiversResult, hash };
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
    setShowViolationsDetail: propSet('showViolationsDetail'),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default componentDetailsViolationsSlice.reducer;
export const actions = {
  ...componentDetailsViolationsSlice.actions,
  load,
  goToWaivers,
};
