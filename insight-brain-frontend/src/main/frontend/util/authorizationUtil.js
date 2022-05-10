/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getGlobalPermissionTestUrl } from '../utilAngular/CLMContextLocation';

export const authErrorMessage = `It appears you do not have permission to access this page.
  If you believe this to be incorrect please contact your administrator.`;

/**
 * Given a list of permissions to check, returns promise that:
 * - resolves with no value if authorized for all provided permissions
 * - rejects with authErrorMessage if not authorized for at least one permission
 *
 * this utility is meant to be used in async action creators:
 * <pre>
 *     return checkPermissions(['CONFIGURE_SYSTEM'])
 *       .then(load)
 *       .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
 * </pre>
 *
 * @param permissions - list of permissions to check
 * @returns {Promise}
 */
export function checkPermissions(permissions) {
  return getPermissions(permissions).then((data) => {
    if (data.length !== permissions.length) {
      throw authErrorMessage;
    }
  });
}

/**
 * Given a list of permissions to check, returns promise that:
 * - reolves with all permitted permissions from passed list
 *
 * this utility is meant to be used in async action creators:
 * <pre>
 *     return getPermissions(['CONFIGURE_SYSTEM'])
 *       .then(load)
 *       .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
 * </pre>
 *
 * @param permissions - list of permissions to verify
 * @returns {Promise}
 */
export function getPermissions(permissions) {
  return axios.put(getGlobalPermissionTestUrl(), permissions).then(({ data }) => {
    return data;
  });
}
