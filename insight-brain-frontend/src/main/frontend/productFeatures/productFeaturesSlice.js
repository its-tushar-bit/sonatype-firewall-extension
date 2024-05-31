/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { isEmpty, prop, pipe, equals, includes } from 'ramda';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import {
  getProductFeaturesUrl,
  getEnableUnauthenticatedPages,
  getEnableSsoOnly,
  getQuarantinedComponentViewAnonymousAccessEnabledState,
  getOAuth2Enabled,
} from 'MainRoot/util/CLMLocation';
import { selectProductFeatures } from './productFeaturesSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';

const REDUCER_NAME = 'productFeatures';

export const initialState = {
  loading: false,
  loadError: null,
  productFeatures: {},
};

const fetchProductFeaturesIfNeededFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.productFeatures = { ...state.productFeatures, ...payload };
};

const fetchProductFeaturesIfNeededPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const fetchProductFeaturesIfNeededRejected = (state, { payload }) => {
  state.loading = true;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const fetchProductFeatures = createAsyncThunk(`${REDUCER_NAME}/fetchProductFeatures`, (_, { rejectWithValue }) => {
  return axios.get(getProductFeaturesUrl()).then(prop('data')).catch(rejectWithValue);
});

const loadIsQuarantinedComponentViewAnonymousAccessEnabled = createAsyncThunk(
  `${REDUCER_NAME}/loadIsQuarantinedComponentViewAnonymousAccessEnabled`,
  (_, { rejectWithValue }) =>
    axios
      .get(getQuarantinedComponentViewAnonymousAccessEnabledState())
      .then(pipe(prop('data'), equals(true)))
      .catch(rejectWithValue)
);

const fetchProductFeaturesIfNeeded = createAsyncThunk(
  `${REDUCER_NAME}/fetchProductFeaturesIfNeeded`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const productFeatures = selectProductFeatures(getState());
    const promise = isEmpty(productFeatures) ? dispatch(fetchProductFeatures()) : Promise.resolve({});
    return promise
      .then((featuresPayload) => {
        if (isEmpty(productFeatures)) {
          const features = unwrapResult(featuresPayload);
          let productFeatures = {};

          features.forEach((feature) => {
            productFeatures[feature] = true;
          });

          return productFeatures;
        }
      })
      .catch(rejectWithValue);
  }
);

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsUnauthenticatedPagesEnabled = createAsyncThunk(`${REDUCER_NAME}/loadIsUnauthenticatedPagesEnabled`, () =>
  axios.get(getEnableUnauthenticatedPages()).then(pipe(prop('data'), includes('enable-unauthenticated-pages')))
);

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsSsoOnlyEnabled = createAsyncThunk(`${REDUCER_NAME}/loadIsSsoOnlyEnabled`, () =>
  axios.get(getEnableSsoOnly()).then(pipe(prop('data'), includes('enable-sso-only')))
);

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsOauth2Enabled = createAsyncThunk(`${REDUCER_NAME}/loadIsOauth2Enabled`, () =>
  axios.get(getOAuth2Enabled()).then(pipe(prop('data'), includes('oauth2-enabled')))
);

const productFeaturesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [fetchProductFeaturesIfNeeded.fulfilled]: fetchProductFeaturesIfNeededFulfilled,
    [fetchProductFeaturesIfNeeded.pending]: fetchProductFeaturesIfNeededPending,
    [fetchProductFeaturesIfNeeded.rejected]: fetchProductFeaturesIfNeededRejected,
  },
});

export default productFeaturesSlice.reducer;
export const actions = {
  ...productFeaturesSlice.actions,
  fetchProductFeaturesIfNeeded,
  loadIsQuarantinedComponentViewAnonymousAccessEnabled,
  loadIsUnauthenticatedPagesEnabled,
  loadIsSsoOnlyEnabled,
  loadIsOauth2Enabled,
};
