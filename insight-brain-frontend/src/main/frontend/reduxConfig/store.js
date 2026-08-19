/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { configureStore } from '@reduxjs/toolkit';
import routerMiddleware from 'MainRoot/reduxUiRouter/routerMiddleware';
import reducer from './reducers';

const middleware = [routerMiddleware];

if (process.env.NODE_ENV === 'development' && window.console && window.console.log.apply) {
  const createLogger = require('redux-logger').createLogger;
  const logger = createLogger({ level: 'info', collapsed: true, diff: false });
  middleware.push(logger);
}

const store = configureStore({
  reducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
      immutableCheck: false,
    }).concat(middleware),
  devTools: process.env.NODE_ENV !== 'production',
});

export default store;
