/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getReportAuditLogUrl } from '../../util/CLMLocation';
import { httpErrorMessageActionCreator, noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { sortItemsByFields } from '../../util/sortUtils';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';

export const AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED = 'AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED';
export const AUDIT_LOG_LOAD_AUDIT_LOG_FAILED = 'AUDIT_LOG_LOAD_AUDIT_LOG_FAILED';
export const AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED = 'AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED';
export const AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED = 'AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED';

const loadAuditLogRequested = noPayloadActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED);
const loadAuditLogFulfilled = payloadParamActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED);
const loadAuditLogFailed = httpErrorMessageActionCreator(AUDIT_LOG_LOAD_AUDIT_LOG_FAILED);

export function loadAuditLogForComponent() {
  return (dispatch, getState) => {
    dispatch(loadAuditLogRequested());
    const state = getState();
    const selectedComponent = selectSelectedComponent(state);

    const {
      router: {
        currentParams: { publicId, scanId },
      },
    } = state;
    const url = getReportAuditLogUrl(publicId, scanId, selectedComponent);

    return axios
      .get(url)
      .then(({ data }) => {
        const response = data.aaData || [];
        dispatch(loadAuditLogFulfilled(response));
        if (response && response.length) {
          dispatch(sortAuditLog());
        }
      })
      .catch((error) => {
        dispatch(loadAuditLogFailed(error));
      });
  };
}

const sortAuditLogRequested = payloadParamActionCreator(AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED);
const sortAuditLogFulfilled = payloadParamActionCreator(AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED);

export function sortAuditLog(sortField = '-time') {
  return (dispatch, getState) => {
    dispatch(sortAuditLogRequested(sortField));
    const {
      auditLog: { auditRecords },
    } = getState();
    const sortedResults = sortItemsByFields([sortField], auditRecords);
    dispatch(sortAuditLogFulfilled(sortedResults));
  };
}
