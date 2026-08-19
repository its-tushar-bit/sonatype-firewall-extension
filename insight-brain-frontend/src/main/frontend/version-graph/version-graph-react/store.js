/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { configureStore } from '@reduxjs/toolkit';

import applicationsReducer from './slices/applicationsSlice';
import componentsReducer from './slices/componentsSlice';
import componentDetailsReducer from './slices/componentDetailsSlice';
import globalReducer from './slices/globalSlice';

const createStore = () =>
  configureStore({
    reducer: {
      applications: applicationsReducer,
      components: componentsReducer,
      componentDetails: componentDetailsReducer,
      global: globalReducer,
    },
  });

// The singleton store instance
let store = createStore();

/**
 * Reset the store with a fresh instance - for testing purposes only
 * @returns {Object} The newly created store instance
 */
export function _resetForTests() {
  store = createStore();
  return store;
}

export default store;
