/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSlice';

describe('EnterpriseReportingSupportInfoReducer', () => {
  let initialState;

  const fakeTelemetryData = {
    telemetryId: '12345',
    clusterId: '12345-678',
    advancedReportingEnabled: true,
    enterpriseReportingFeatureExists: true,
    userApplicationCount: 50,
    totalApplicationCount: 100,
  };

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('enterpriseReportingSupportInfo/load/pending', () => {
    it('should return the initial state', () => {
      const action = { type: 'enterpriseReportingSupportInfo/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual(initialState);
    });
  });

  describe('enterpriseReportingSupportInfo/load/fulfilled', () => {
    it('should set the telemetry data to the state ', () => {
      const oldState = Object.freeze({
        loading: false,
        loadError: null,
      });
      const newState = reducer(oldState, {
        type: 'enterpriseReportingSupportInfo/load/fulfilled',
        payload: fakeTelemetryData,
      });

      expect(newState).toEqual({
        loading: false,
        loadError: null,
        telemetryStatus: fakeTelemetryData,
      });
    });
  });

  describe('enterpriseReportingSupportInfo/load/rejected', () => {
    it('should set the data to the state ', () => {
      const oldState = Object.freeze({ loading: false, telemetryStatus: {} });
      const newState = reducer(oldState, {
        type: 'enterpriseReportingSupportInfo/load/rejected',
        payload: 'Error on load',
      });

      expect(newState).toEqual({
        loadError: 'Error on load',
        loading: false,
        telemetryStatus: {},
      });
    });
  });
});
