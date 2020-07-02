/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import { ADD_WAIVER } from './addWaiverActions';

// ToDo: Use real actions and derive proper state
const initialState = Object.freeze({
  waivers: []
});

const reducerActionMap = {
  [ADD_WAIVER]: addWaiver
};

function addWaiver() {
  return initialState;
}

const addWaiverReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default addWaiverReducer;
