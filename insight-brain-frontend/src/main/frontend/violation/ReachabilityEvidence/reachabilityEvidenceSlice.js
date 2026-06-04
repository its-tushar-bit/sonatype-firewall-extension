/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { getReachabilityEvidenceUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'reachabilityEvidence';

export const initialState = {
  loading: false,
  loadError: null,
  evidence: null,
  isOpen: false,
  currentRequestId: null,
};

const loadEvidenceFulfilled = (state, { payload, meta }) => {
  // Ignore stale responses from previous requests (race condition guard)
  if (state.currentRequestId !== meta.requestId) {
    return;
  }
  state.loading = false;
  state.loadError = null;
  state.evidence = payload;
  state.currentRequestId = null;
};

const loadEvidenceFailed = (state, { payload, meta }) => {
  // Ignore stale responses from previous requests (race condition guard)
  if (state.currentRequestId !== meta.requestId) {
    return;
  }
  state.loading = false;
  state.currentRequestId = null;
  // 404 means no evidence available, not an error — use empty sentinel to prevent re-fetching
  if (payload?.response?.status === 404) {
    state.loadError = null;
    state.evidence = { paths: [], truncated: false };
  } else {
    state.loadError = 'Failed to load reachability evidence';
  }
};

const loadEvidencePending = (state, { meta }) => {
  state.loading = true;
  state.loadError = null;
  state.currentRequestId = meta.requestId;
};

export const loadEvidence = createAsyncThunk(
  `${REDUCER_NAME}/loadEvidence`,
  ({ applicationPublicId, reportId, vulnerabilityId }, { rejectWithValue }) => {
    return axios
      .get(getReachabilityEvidenceUrl(applicationPublicId, reportId, vulnerabilityId))
      .then(({ data }) => data)
      .catch(rejectWithValue);
  }
);

function toggleAccordion(state) {
  state.isOpen = !state.isOpen;
}

function reset() {
  return { ...initialState };
}

const reachabilityEvidenceSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    toggleAccordion,
    reset,
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadEvidence.pending, loadEvidencePending)
      .addCase(loadEvidence.fulfilled, loadEvidenceFulfilled)
      .addCase(loadEvidence.rejected, loadEvidenceFailed);
  },
});

export default reachabilityEvidenceSlice.reducer;

export const actions = {
  ...reachabilityEvidenceSlice.actions,
};
