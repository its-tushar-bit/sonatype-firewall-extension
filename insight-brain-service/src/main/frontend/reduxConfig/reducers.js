/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {combineReducers} from 'redux';
import routerStateReducer from '../reduxUiRouter/routerStateReducer';
import dashboardReducer from '../dashboard/dashboardReducer';

export default combineReducers({
  router: routerStateReducer,
  dashboard: dashboardReducer
});
