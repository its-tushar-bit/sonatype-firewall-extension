/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { head as first } from 'ramda';
import { createSlice } from '@reduxjs/toolkit';

import { next, prev, steps } from './firewallOnboardingUtils';

export const REDUCER_NAME = 'firewallOnboarding';

export const initialState = {
  loading: false,
  currentStep: first(steps),
};

const continueToNextStep = (state) => {
  if (next(state.currentStep)) {
    state.currentStep = next(state.currentStep);
  }
};

const goBackToPreviousStep = (state) => {
  if (prev(state.currentStep)) {
    state.currentStep = prev(state.currentStep);
  }
};

const firewallOnboardingSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    continueToNextStep,
    goBackToPreviousStep,
  },
  extraReducers: {},
});

export const actions = {
  ...firewallOnboardingSlice.actions,
};

export default firewallOnboardingSlice.reducer;
