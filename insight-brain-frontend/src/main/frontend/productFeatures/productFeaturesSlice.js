/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { isEmpty, prop, pipe, equals, includes } from 'ramda';
import { createAsyncThunk, unwrapResult } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import {
  getProductFeaturesUrl,
  getEnableUnauthenticatedPages,
  getEnableSsoOnly,
  getQuarantinedComponentViewAnonymousAccessEnabledState,
  getOAuth2Enabled,
} from 'MainRoot/util/CLMLocation';
import { selectProductFeatures } from './productFeaturesSelectors';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'productFeatures';

export const initialState = {
  loading: false,
  loadError: null,
  productFeatures: {},
  isEnterprisePreviewMode: false,
  dismissedPopovers: {},
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
  state.loading = false;
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
 * Ensures the product-features map is populated, for callers that only need to read a
 * feature flag rather than react to the fetch.
 *
 * `fetchProductFeaturesIfNeeded` skips the network round trip when the map is already
 * populated, but still dispatches its `pending` action, flipping `loading` true→false.
 * Consumers gated on that flag blank themselves while it is true — `IqSidebarNav` renders
 * `DefaultEmptyIqSidebar`, and `ReportingRoute` guards against a remount loop — so a
 * caller that dispatches it purely to read a flag causes a visible flicker.
 *
 * The map is cached for the session once fetched, so a feature toggled server-side
 * mid-session is not reflected until a page refresh re-triggers the fetch. That is
 * inherited from `fetchProductFeaturesIfNeeded` rather than specific to this helper.
 *
 * TODO: concurrent first callers are not deduped. Each can observe an empty map and
 * dispatch `fetchProductFeaturesIfNeeded`, costing one redundant (200) GET at login.
 * `createAsyncThunk`'s `condition` option is not a usable fix here: it resolves with a
 * `rejected` action carrying `meta.condition`, which breaks callers chaining
 * `.then(unwrapResult)`.
 */
const loadProductFeaturesOnce = () => async (dispatch, getState) => {
  if (isEmpty(selectProductFeatures(getState()))) {
    await dispatch(fetchProductFeaturesIfNeeded());
  }
};

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsUnauthenticatedPagesEnabled = createAsyncThunk(`${REDUCER_NAME}/loadIsUnauthenticatedPagesEnabled`, () =>
  axios
    .get(getEnableUnauthenticatedPages(), { waitForLogin: false })
    .then(pipe(prop('data'), includes('enable-unauthenticated-pages')))
);

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsSsoOnlyEnabled = createAsyncThunk(`${REDUCER_NAME}/loadIsSsoOnlyEnabled`, () =>
  axios.get(getEnableSsoOnly(), { waitForLogin: false }).then(pipe(prop('data'), includes('enable-sso-only')))
);

/**
 * Separate REST call because it must be accessible before login
 */
const loadIsOauth2Enabled = createAsyncThunk(`${REDUCER_NAME}/loadIsOauth2Enabled`, () =>
  axios.get(getOAuth2Enabled(), { waitForLogin: false }).then(pipe(prop('data'), includes('oauth2-enabled')))
);

const setEnterprisePreviewMode = (state, { payload }) => {
  state.isEnterprisePreviewMode = payload;
};

const dismissPopover = (state, { payload: featureId }) => {
  state.dismissedPopovers[featureId] = true;
};

const productFeaturesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setEnterprisePreviewMode,
    dismissPopover,
  },
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
  loadProductFeaturesOnce,
  loadIsQuarantinedComponentViewAnonymousAccessEnabled,
  loadIsUnauthenticatedPagesEnabled,
  loadIsSsoOnlyEnabled,
  loadIsOauth2Enabled,
};
