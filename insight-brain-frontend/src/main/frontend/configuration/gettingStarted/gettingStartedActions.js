/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose } from 'ramda';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getIsHdsReachable, getLicenseSummaryUrl } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
import { getPermissions } from '../../util/authorizationUtil';

export const GETTING_STARTED_LOAD_REQUESTED = 'GETTING_STARTED_LOAD_REQUESTED';
export const GETTING_STARTED_LOAD_FULFILLED = 'GETTING_STARTED_LOAD_FULFILLED';
export const GETTING_STARTED_LOAD_FAILED = 'GETTING_STARTED_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(GETTING_STARTED_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(GETTING_STARTED_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(GETTING_STARTED_LOAD_FAILED);

export const permissions = ['CONFIGURE_SYSTEM', 'ADD_APPLICATION'];

const isAdmin = (validPermissions) => validPermissions.indexOf('CONFIGURE_SYSTEM') >= 0;

export function load() {
  return (dispatch) => {
    dispatch(loadRequested());
    return getPermissions(permissions)
      .then((validPermissions) => {
        const payload = {};
        const loadIsHdsReachable = axios.get(getIsHdsReachable());
        const loadLicenseSummaryUrl = axios.get(getLicenseSummaryUrl());

        const promises = [loadIsHdsReachable];

        if (validPermissions.indexOf('CONFIGURE_SYSTEM') >= 0) {
          promises.push(loadLicenseSummaryUrl);
        }

        payload.validPermissions = validPermissions;
        payload.isAuthorizedToViewSystemSetup = validPermissions.length > 0;

        return Promise.all(promises).then(([hdsReachableResults, licenseResult]) => {
          const {
            data: { alive, errorMessage, incidentId },
          } = hdsReachableResults;

          payload.shouldDisplayHdsUnreachable = !alive;
          payload.hdsUnreachableErrorMessage = errorMessage;
          payload.hdsUnreachableIncidentId = incidentId;

          if (licenseResult) {
            const license = licenseResult.data;
            payload.license = license;
            payload.isAdmin = isAdmin(validPermissions);
          }

          dispatch(loadFulfilled(payload));
        });
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}
