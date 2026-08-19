/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice } from '@reduxjs/toolkit';

const REDUCER_NAME = 'firewallBulkWaiver';

const initialState = {
  selectedViolations: [],
  selectedCount: 0,
  selectAllMode: false,
  checkboxState: {},
  waiverConfiguration: {
    waiverReasonId: '',
    expiryTime: '',
    comments: '',
    componentMatcherStrategy: null,
    selectedWaiverScope: null,
  },
  waiverReasons: [],
  loadingWaiverReasons: false,
  waiverReasonsError: null,
  availableWaiverScopes: [],
  loadingWaiverScopes: false,
  waiverScopesError: null,
  selectedWaiverScope: null,
  allFilteredViolations: [],
  loadingAllViolations: false,
  allViolationsError: null,
  totalFilteredCount: 0,
  submitting: false,
  submitSuccess: false,
  submitError: null,
  sourceContext: {
    source: null,
    repositoryId: null,
    componentIdentifier: null,
    componentHash: null,
    matchState: null,
    tabId: null,
    pathname: null,
    componentDisplayName: null,
  },
  originalAggregateState: null,
  componentDetailsPolicyNameFilter: '',
  componentDetailsConstraintNameFilter: '',
};

const setSelectedViolations = (state, action) => {
  state.selectedViolations = action.payload;
};

const setSelectedCount = (state, action) => {
  state.selectedCount = action.payload;
};

const setSelectAllMode = (state, action) => {
  state.selectAllMode = action.payload;
};

const setCheckboxState = (state, action) => {
  state.checkboxState = action.payload;
};

const setWaiverConfiguration = (state, action) => {
  state.waiverConfiguration = action.payload;
};

const clearWaiverConfiguration = (state) => {
  state.waiverConfiguration = initialState.waiverConfiguration;
};

const setWaiverReasons = (state, action) => {
  state.waiverReasons = action.payload;
  state.loadingWaiverReasons = false;
  state.waiverReasonsError = null;
};

const setLoadingWaiverReasons = (state, action) => {
  state.loadingWaiverReasons = action.payload;
};

const setWaiverReasonsError = (state, action) => {
  state.waiverReasonsError = action.payload;
  state.loadingWaiverReasons = false;
};

const setAvailableWaiverScopes = (state, action) => {
  state.availableWaiverScopes = action.payload;
  state.loadingWaiverScopes = false;
  state.waiverScopesError = null;
};

const setLoadingWaiverScopes = (state, action) => {
  state.loadingWaiverScopes = action.payload;
};

const setWaiverScopesError = (state, action) => {
  state.waiverScopesError = action.payload;
  state.loadingWaiverScopes = false;
};

const setSelectedWaiverScope = (state, action) => {
  state.selectedWaiverScope = action.payload;
};

const setAllFilteredViolations = (state, action) => {
  state.allFilteredViolations = action.payload;
  state.loadingAllViolations = false;
  state.allViolationsError = null;
  state.totalFilteredCount = action.payload.length;
};

const setLoadingAllViolations = (state, action) => {
  state.loadingAllViolations = action.payload;
};

const setAllViolationsError = (state, action) => {
  state.allViolationsError = action.payload;
  state.loadingAllViolations = false;
};

const setSourceContext = (state, action) => {
  state.sourceContext = action.payload;
};

const clearSourceContext = (state) => {
  state.sourceContext = initialState.sourceContext;
};

const setSubmitting = (state, action) => {
  state.submitting = action.payload;
};

const setSubmitSuccess = (state, action) => {
  state.submitSuccess = action.payload;
  state.submitting = false;
  state.submitError = null;
};

const setSubmitError = (state, action) => {
  state.submitError = action.payload;
  state.submitting = false;
  state.submitSuccess = false;
};

const setOriginalAggregateState = (state, action) => {
  state.originalAggregateState = action.payload;
};

const clearOriginalAggregateState = (state) => {
  state.originalAggregateState = null;
};

const setComponentDetailsPolicyNameFilter = (state, action) => {
  state.componentDetailsPolicyNameFilter = action.payload;
};

const setComponentDetailsConstraintNameFilter = (state, action) => {
  state.componentDetailsConstraintNameFilter = action.payload;
};

const firewallBulkWaiverSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setSelectedViolations,
    setSelectedCount,
    setSelectAllMode,
    setCheckboxState,
    setWaiverConfiguration,
    clearWaiverConfiguration,
    setWaiverReasons,
    setLoadingWaiverReasons,
    setWaiverReasonsError,
    setAvailableWaiverScopes,
    setLoadingWaiverScopes,
    setWaiverScopesError,
    setSelectedWaiverScope,
    setAllFilteredViolations,
    setLoadingAllViolations,
    setAllViolationsError,
    setSourceContext,
    clearSourceContext,
    setSubmitting,
    setSubmitSuccess,
    setSubmitError,
    setOriginalAggregateState,
    clearOriginalAggregateState,
    setComponentDetailsPolicyNameFilter,
    setComponentDetailsConstraintNameFilter,
  },
});

export const actions = firewallBulkWaiverSlice.actions;
export default firewallBulkWaiverSlice.reducer;
