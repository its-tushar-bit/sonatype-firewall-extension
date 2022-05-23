/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, curry, lensPath, lensProp, set } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { Messages } from '../utilAngular/CommonServices';
/*
 * like `./jsUtil.js#propSet` but is meant to be partially applied in 2 args.
 * The payload is ignored and is only an argument to conform to the interface needed by reducerActionMap
 */
export const propSetConst = curry((propName, constValue, payload, state) => set(lensProp(propName), constValue, state));

export const pathSetConst = curry((propPath, constValue, payload, state) => set(lensPath(propPath), constValue, state));

/**
 * A generic reducer function parameterized over a reducerActionMap and an initialState.
 * Works by looking up the action type in the reducerAction map
 * and then executing the found function
 *
 * @param reducerActionMap: an object/dictionary of 'actions':'functions',
 *  where `actions` are action type strings
 *  and the functions are with signature `(payload, state) => state`; that is, reducer functions.
 * @param initialState: the initial state of the app/reducer.
 */
export function createReducerFromActionMap(reducerActionMap, initialState) {
  return (state = initialState, action) => {
    const type = action && action.type,
      payload = action && action.payload,
      reducer = type && reducerActionMap[type];

    return reducer ? reducer(payload, state) : state;
  };
}

/**
 * Some convenience functions for common, simple types of redux action creators
 */
export const noPayloadActionCreator = (type) => always({ type });
export const payloadParamActionCreator = (type) => (payload) => ({
  type,
  payload,
});
export const mappedPayloadParamActionCreator = (type, mapper) => (payloadSrc) => ({
  type,
  payload: mapper(payloadSrc),
});

export function httpErrorMessageActionCreator(type) {
  return mappedPayloadParamActionCreator(type, Messages.getHttpErrorMessage);
}

export const toggleBooleanProp = (propName) => (state) => {
  return {
    ...state,
    [propName]: !state[propName],
  };
};

export const startSaveMaskSuccessTimer = (dispatch, actionFunc) =>
  new Promise((res) =>
    setTimeout(() => {
      res(dispatch(actionFunc()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS)
  );
