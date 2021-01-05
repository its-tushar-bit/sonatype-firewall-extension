/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getLegalDashboardApplicationsUrl } from '../../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';

export const LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED = 'LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED';
export const LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED = 'LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED';
export const LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED = 'LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED';

const loadApplicationsRequested = noPayloadActionCreator(LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED);
const loadApplicationsFulfilled = payloadParamActionCreator(LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED);
const loadApplicationsFailed = payloadParamActionCreator(LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED);

export function loadApplications() {
  return (dispatch) => {
    dispatch(loadApplicationsRequested());

    const applicationFilter = {
      applicationIds: [],
      organizationIds: [],
      stageTypeIds: [],
      tagIds: []
    };

    return axios.post(getLegalDashboardApplicationsUrl(), applicationFilter)
        .then(({ data }) => {
          dispatch(loadApplicationsFulfilled(data));
        })
        .catch(error => {
          dispatch(loadApplicationsFailed(error));
        });
  };
}
