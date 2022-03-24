/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { isEmpty, prop } from 'ramda';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';
import { selectProductFeaturesSlice } from './productFeaturesSelectors';

const REDUCER_NAME = 'productFeatures';

export const initialState = {};

const fetchProductFeaturesIfNeededFullfilled = (state, { payload }) => {
  if (payload) {
    return {
      ...state,
      ...payload,
    };
  }
  return state;
};

const fetchProductFeatures = createAsyncThunk(`${REDUCER_NAME}/fetchProductFeatures`, (_, { rejectWithValue }) => {
  return axios.get(getProductFeaturesUrl()).then(prop('data')).catch(rejectWithValue);
});

const fetchProductFeaturesIfNeeded = createAsyncThunk(
  `${REDUCER_NAME}/fetchProductFeaturesIfNeeded`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const productFeatures = selectProductFeaturesSlice(getState());
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

const productFeaturesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  extraReducers: {
    [fetchProductFeaturesIfNeeded.fulfilled]: fetchProductFeaturesIfNeededFullfilled,
  },
});

export default productFeaturesSlice.reducer;
export const actions = {
  ...productFeaturesSlice.actions,
  fetchProductFeaturesIfNeeded,
};
