/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  SUCCESS_METRICS_REPORT_LOAD_REQUESTED,
  SUCCESS_METRICS_REPORT_LOAD_FAILED,
  SUCCESS_METRICS_REPORT_LOAD_FULFILLED,
  SUCCESS_METRICS_REPORT_DELETE_REQUESTED,
  SUCCESS_METRICS_REPORT_DELETE_FULFILLED,
  SUCCESS_METRICS_REPORT_DELETE_FAILED,
  SUCCESS_METRICS_DELETE_MASK_TIMER_DONE,
} from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/SuccessMetricsReportActions';
import reducer from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/SuccessMetricsReportReducer';

describe('successMetricsReportReducer', () => {
  let initialState;

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  it('initial state has default field values', function () {
    expect(initialState.loading).toBe(true);
    expect(initialState.loadError).toBe(null);
    expect(initialState.mttrs).toEqual([]);
    expect(initialState.averages).toEqual({});
    expect(initialState.applicationCounts).toEqual({});
    expect(initialState.violationCounts).toEqual([]);
    expect(initialState.violationsByCategoryWeeks).toEqual([]);
    expect(initialState.lastUpdated).toBe(null);
    expect(initialState.monthCount).toBe(null);
    expect(initialState.reportName).toBe('');
    expect(initialState.isSingleApplicationReport).toBe(null);
    expect(initialState.singleApplicationName).toBe(null);
    expect(initialState.includeLatestData).toBe(null);
    expect(initialState.componentCounts).toBe(null);
    expect(initialState.deleteMaskState).toBe(null);
    expect(initialState.deleteError).toBe(null);
  });

  describe('SUCCESS_METRICS_REPORT_LOAD_REQUESTED', function () {
    it('returns initial state', function () {
      const action = { type: SUCCESS_METRICS_REPORT_LOAD_REQUESTED };
      const newState = reducer(undefined, action);

      expect(newState).toEqual(initialState);
    });
  });
  describe('SUCCESS_METRICS_REPORT_LOAD_FAILED', function () {
    it('set loadError', function () {
      const errorMsg = 'error test';
      const action = { type: SUCCESS_METRICS_REPORT_LOAD_FAILED, payload: errorMsg };
      const newState = reducer(initialState, action);

      expect(newState.loadError).toBe(errorMsg);
    });
  });
  describe('SUCCESS_METRICS_REPORT_LOAD_FULFILLED', function () {
    let newState, payload;

    beforeAll(() => {
      payload = { lastUpdated: 101, monthCount: 0, singleApplicationName: '101' };
      const action = { type: SUCCESS_METRICS_REPORT_LOAD_FULFILLED, payload };
      newState = reducer(initialState, action);
    });

    it('sets loading', function () {
      expect(newState.loading).toBe(false);
    });
    it('sets lastUpdated', function () {
      expect(newState.lastUpdated).toBe(payload.lastUpdated);
    });
    it('sets monthCount', function () {
      expect(newState.monthCount).toBe(payload.monthCount);
    });
    it('sets singleApplicationName', function () {
      expect(newState.singleApplicationName).toBe(payload.singleApplicationName);
    });
  });

  describe('SUCCESS_METRICS_REPORT_DELETE_REQUESTED', function () {
    it('sets deleteMaskState to false', function () {
      const action = { type: SUCCESS_METRICS_REPORT_DELETE_REQUESTED };
      const newState = reducer(initialState, action);

      expect(newState.deleteMaskState).toBe(false);
    });
  });
  describe('SUCCESS_METRICS_REPORT_DELETE_FULFILLED', function () {
    it('sets deleteMaskState to true and deleteError to null', function () {
      const action = { type: SUCCESS_METRICS_REPORT_DELETE_FULFILLED };
      const newState = reducer({ ...initialState, deleteError: 'error test' }, action);

      expect(newState.deleteMaskState).toBe(true);
      expect(newState.deleteError).toBe(null);
    });
  });
  describe('SUCCESS_METRICS_REPORT_DELETE_FAILED', function () {
    it('sets deleteMaskState to null and deleteError to payload', function () {
      const errorMsg = 'error test';
      const action = { type: SUCCESS_METRICS_REPORT_DELETE_FAILED, payload: errorMsg };
      const newState = reducer({ ...initialState, deleteMaskState: false }, action);

      expect(newState.deleteMaskState).toBe(null);
      expect(newState.deleteError).toBe(errorMsg);
    });
  });
  describe('SUCCESS_METRICS_DELETE_MASK_TIMER_DONE', function () {
    it('set loadError', function () {
      const action = { type: SUCCESS_METRICS_DELETE_MASK_TIMER_DONE };
      const newState = reducer({ ...initialState, deleteMaskState: false }, action);

      expect(newState.deleteMaskState).toBe(null);
    });
  });
});
