/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose } from 'ramda';
import { Messages } from '../utilAngular/CommonServices';
import { getPermissions, authErrorMessage } from '../util/authorizationUtil';

import { getRoleListUrl } from '../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';

export const permissions = ['VIEW_ROLES', 'EDIT_ROLES'];

export const ROLES_LIST_LOAD_REQUESTED = 'ROLES_LIST_LOAD_REQUESTED';
export const ROLES_LIST_LOAD_FULFILLED = 'ROLES_LIST_LOAD_FULFILLED';
export const ROLES_LIST_LOAD_FAILED = 'ROLES_LIST_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(ROLES_LIST_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(ROLES_LIST_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(ROLES_LIST_LOAD_FAILED);

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());

    return getPermissions(permissions)
      .then((data) => {
        if (!data.includes('VIEW_ROLES')) {
          throw authErrorMessage;
        }
        const readOnly = !data.includes('EDIT_ROLES');
        return axios.get(getRoleListUrl()).then(({ data }) => {
          dispatch(loadFulfilled({ roles: data, readOnly }));
        });
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}
