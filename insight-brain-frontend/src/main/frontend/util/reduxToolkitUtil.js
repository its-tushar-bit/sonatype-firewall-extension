/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { curry, set, lensProp, lensPath } from 'ramda';

import { propSet as jsUtilPropSet, pathSet as jsUtilPathSet } from './jsUtil';

/*
 * like `reduxUtil#propSetConst` but for use in redux-toolkit as opposed to our homegrown createReducerFromActionMap
 * function. The only difference is that the payload and state arguments are switched
 */
export const propSetConst = curry((propName, constValue, state) => set(lensProp(propName), constValue, state));

/*
 * like `reduxUtil#pathSetConst` but for use in redux-toolkit as opposed to our homegrown createReducerFromActionMap
 * function. The only difference is that the payload and state arguments are switched
 */
export const pathSetConst = curry((pathName, constValue, state) => set(lensPath(pathName), constValue, state));

/*
 * Like ./jsUtil#propSet except it takes the second and third arguments as redux would pass them.
 * ((state, action) rather than (payload, state))
 */
export const propSet = curry((propName, state, { payload }) => jsUtilPropSet(propName, payload, state));

/*
 * Like ./jsUtil#pathSet except it takes the second and third arguments as redux would pass them.
 * ((state, action) rather than (payload, state))
 */
export const pathSet = curry((path, state, { payload }) => jsUtilPathSet(path, payload, state));
