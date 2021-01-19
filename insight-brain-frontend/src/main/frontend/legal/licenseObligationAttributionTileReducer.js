/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  LICENSE_OBLIGATION_SET_ATTRIBUTION_TEXT,
  LICENSE_OBLIGATION_SET_OBLIGATION_FULFILLED,
  LICENSE_OBLIGATION_SET_SCOPE
} from './licenseObligationAttributionTileActions';
import { pathSet } from '../util/jsUtil';
import { createReducerFromActionMap } from '../util/reduxUtil';
import { TEXT_BASED_OBLIGATIONS } from './advancedLegalConstants';

const initialState = {};
TEXT_BASED_OBLIGATIONS.forEach(key => initialState[key] = {
  attributionText: '',
  obligationFulfilled: false,
  scope: 'ROOT_ORGANIZATION_ID'
});

const reducerActionMap = {
  [LICENSE_OBLIGATION_SET_ATTRIBUTION_TEXT]: (payload, state) =>
    pathSet([payload.name, 'attributionText'], payload.value, state),
  [LICENSE_OBLIGATION_SET_OBLIGATION_FULFILLED]: (payload, state) =>
    pathSet([payload.name, 'obligationFulfilled'], payload.value, state),
  [LICENSE_OBLIGATION_SET_SCOPE]: (payload, state) =>
    pathSet([payload.name, 'scope'], payload.value, state)
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
