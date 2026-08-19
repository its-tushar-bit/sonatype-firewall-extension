/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, map } from 'ramda';

import {
  AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED,
  AUDIT_LOG_LOAD_AUDIT_LOG_FAILED,
  AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED,
  AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED,
} from './auditLogActions';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { createReducerFromActionMap, propSetConst } from 'MainRoot/util/reduxUtil';
import { processAuditRecord } from '../componentDetailsUtils';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';

export const initState = Object.freeze({
  isLoading: false,
  auditRecords: [],
  error: null,
  appliedSort: null,
});

const reducerActionMap = {
  [AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED]: propSetConst('isLoading', true),
  [AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED]: loadAuditLogFulfilled,
  [AUDIT_LOG_LOAD_AUDIT_LOG_FAILED]: loadAuditLogFailed,
  [AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED]: sortAuditLogRequested,
  [AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED]: sortAuditLogFulfilled,
  [UI_ROUTER_ON_FINISH]: always(initState),
  [SELECT_COMPONENT]: always(initState),
};

function loadAuditLogFailed(payload, state) {
  return { ...state, isLoading: false, error: payload };
}

function loadAuditLogFulfilled(payload, state) {
  const auditRecords = map(processAuditRecord, payload);

  return {
    ...state,
    error: null,
    isLoading: false,
    auditRecords,
  };
}

function sortAuditLogRequested(payload, state) {
  return { ...state, isLoading: true, appliedSort: payload };
}

function sortAuditLogFulfilled(payload, state) {
  return { ...state, isLoading: false, auditRecords: payload };
}

const reducer = createReducerFromActionMap(reducerActionMap, initState);
export default reducer;
