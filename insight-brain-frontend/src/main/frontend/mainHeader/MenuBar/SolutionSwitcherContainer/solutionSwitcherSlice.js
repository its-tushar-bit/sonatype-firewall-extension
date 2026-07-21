/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { getLicensedSolutionsUrl } from '../../../util/CLMLocation';

const REDUCER_NAME = 'solutionSwitcher';

// Duplicated from guide/license/solutionIds.ts rather than imported, since Guide code and legacy
// IQ UI code must never cross-import (see CLAUDE.md). Keep these in sync if that file's ids change.
const AI_DEVELOPER_SOLUTION_ID = 'aiDeveloper';
const GUIDE_SOLUTION_ID = 'guide';

// The AI Developer product is licensed under two SKUs that report different solution ids: the
// legacy Guide SKU ('guide') and the new AI Developer SKU ('aiDeveloper', added in GUIDE-3124).
// The shared @sonatype/solution-switcher-react-component keys AI Developer as 'guide' in its
// DefaultSolutionsList, so a licensed 'aiDeveloper' is not subtracted from the Explore list and
// AI Developer renders twice (once under My Sonatype Solutions, once under Explore). Canonicalize
// the new id onto 'guide' so the package matches it against the default list and shows a single
// "AI Developer" entry. This also lets the 'guide'-keyed selectors (selectIsAiDeveloperEntitled)
// recognize an AI Developer license.
const canonicalizeAiDeveloperId = (solutions) =>
  Array.isArray(solutions)
    ? solutions.map((solution) =>
        solution?.id === AI_DEVELOPER_SOLUTION_ID ? { ...solution, id: GUIDE_SOLUTION_ID } : solution
      )
    : [];

export const initialState = {
  licensedSolutions: [],
  isFetched: false,
  loading: false,
  loadError: null,
};

const fetchLicensedSolutionsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.licensedSolutions = payload;
};

const fetchLicensedSolutionsPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const fetchLicensedSolutionsRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const fetchLicensedSolutions = createAsyncThunk(`${REDUCER_NAME}/fetchLicensedSolutions`, (_, { rejectWithValue }) => {
  return axios
    .get(getLicensedSolutionsUrl())
    .then(({ data }) => canonicalizeAiDeveloperId(data))
    .catch(rejectWithValue);
});

const solutionSwitcherSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [fetchLicensedSolutions.fulfilled]: fetchLicensedSolutionsFulfilled,
    [fetchLicensedSolutions.pending]: fetchLicensedSolutionsPending,
    [fetchLicensedSolutions.rejected]: fetchLicensedSolutionsRejected,
  },
});

export default solutionSwitcherSlice.reducer;
export const actions = {
  ...solutionSwitcherSlice.actions,
  fetchLicensedSolutions,
};
