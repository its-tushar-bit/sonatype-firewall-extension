/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getComponentLicensesUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
} from '../../../util/CLMLocation';
import { Messages } from '../../../util/CommonServices';
import { propSet } from '../../../util/jsUtil';
import { toggleBooleanProp } from '../../../util/reduxUtil';
import { selectComponentDetailsRequestData } from '../../overview/overviewSelectors';

const REDUCER_NAME = 'componentDetailsLicenseDetectionsTile';

const initialState = {
  licenseOverride: null,
  declaredlicenses: null,
  effectiveLicenses: null,
  observedlicenses: null,
  selectableLicenses: null,
  allLicenses: null,
  loading: false,
  loadError: null,
  showEditLicensesPopover: false,
};

const loadFulfilled = (state, { payload }) => {
  state.licenseOverride = payload.licenseOverride ?? null;
  state.declaredlicenses = payload.declaredlicenses ?? null;
  state.effectiveLicenses = payload.effectiveLicenses ?? null;
  state.observedlicenses = payload.observedlicenses ?? null;
  state.selectableLicenses = payload.selectableLicenses ?? null;
  state.allLicenses = payload.allLicenses ?? null;
  state.loading = false;
  state.loadError = null;
};

function loadFailed(state, { payload }) {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
}

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { getState, rejectWithValue }) => {
  const {
    clientType,
    ownerType,
    ownerId,
    identificationSource,
    componentIdentifier,
    scanId,
  } = selectComponentDetailsRequestData(getState());
  const promises = [
    axios.get(getLicensesWithSyntheticFilterUrl()),
    axios.get(
      getComponentLicensesUrl({
        clientType,
        ownerType,
        ownerId,
        componentIdentifier,
        identificationSource,
        scanId,
      })
    ),
    axios.get(getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier)),
  ];

  return Promise.all(promises)
    .then((results) => {
      const allLicenses = results[0].data;
      const { declaredlicenses, effectiveLicenses, observedlicenses, selectableLicenses } = results[1].data;
      const licenseOverride = results[2].data.licenseOverridesByOwner;
      return {
        licenseOverride,
        declaredlicenses,
        effectiveLicenses,
        observedlicenses,
        selectableLicenses,
        allLicenses,
      };
    })
    .catch(rejectWithValue);
});

const componentDetailsLicenseDetectionsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleShowEditLicensesPopover: toggleBooleanProp('showEditLicensesPopover'),
  },
  extraReducers: {
    [load.pending]: propSet('loading', true),
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
  },
});

export default componentDetailsLicenseDetectionsTileSlice.reducer;
export const actions = {
  ...componentDetailsLicenseDetectionsTileSlice.actions,
  load,
};
