/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';

import { getReportAuditLogUrl } from '../../util/CLMLocation';
import { httpErrorMessageActionCreator, noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';

export const AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED = 'AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FAILED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FAILED';

const loadAuditLogRequested = noPayloadActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
const loadAuditLogFulfilled = payloadParamActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED);
const loadAuditLogFailed = httpErrorMessageActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FAILED);

export function loadAuditLogForComponent() {
  return (dispatch, getState) => {
    dispatch(loadAuditLogRequested());

    const {
      applicationReport: { selectedComponent },
      router: {
        currentParams: { publicId, scanId },
      },
    } = getState();
    const url = getReportAuditLogUrl(publicId, scanId, selectedComponent);

    return axios
      .get(url)
      .then(({ data }) => {
        const response = data.aaData || [];
        dispatch(loadAuditLogFulfilled(response));
      })
      .catch((error) => {
        dispatch(loadAuditLogFailed(error));
      });
  };
}
