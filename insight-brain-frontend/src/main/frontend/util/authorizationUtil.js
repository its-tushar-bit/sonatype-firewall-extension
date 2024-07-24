/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getPermissionContextTestUrl, getProductFeaturesUrl } from './CLMLocation';

export const PERMISSION = {
  READ: 'READ',
  WRITE: 'WRITE',
};

export const authErrorMessage = `It appears you do not have permission to access this page.
  If you believe this to be incorrect please contact your administrator.`;

export const featureNotEnableErrorMessage = `It appears that your current licence do not support this feature(s).
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
export function checkPermissions(permissions, ownerType = 'global', ownerId = 'global') {
  return getPermissions(permissions, ownerType, ownerId).then((data) => {
    if (data.length !== permissions.length) {
      throw authErrorMessage;
    }
  });
}

/**
 * Given a list of permissions to check, returns promise that:
 * - resolves with all permitted permissions from passed list
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
export function getPermissions(permissions, ownerType = 'global', ownerId = 'global', waitForLogin = true) {
  return axios.put(getPermissionContextTestUrl(ownerType, ownerId), permissions, { waitForLogin }).then(({ data }) => {
    return data;
  });
}

/**
 * Given a list of features to check, returns promise that:
 * - resolves with no value if all provided features are supported by current product licence
 * - rejects with featureNotEnableErrorMessage if at least one feature is not supported
 *
 * this utility is meant to be used in async action creators:
 * <pre>
 *     return checkPermissions(['CONFIGURE_SYSTEM'])
 *       .then(load)
 *       .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
 * </pre>
 *
 * @param features - list of features to check
 * @returns {Promise}
 */
export function checkFeatures(features) {
  return getFeatures().then((data) => {
    if (!features.every((feature) => data.includes(feature))) {
      throw featureNotEnableErrorMessage;
    }
  });
}

/**
 * Returns promise that:
 * - resolves with all features that are supported by current product licence
 *
 * this utility is meant to be used in async action creators:
 * <pre>
 *     return getPermissions()
 *       .then(load)
 *       .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
 * </pre>
 *
 * @returns {Promise}
 */
export function getFeatures() {
  return axios.get(getProductFeaturesUrl()).then(({ data }) => {
    return data;
  });
}
