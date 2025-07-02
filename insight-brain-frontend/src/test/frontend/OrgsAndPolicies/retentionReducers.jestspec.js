/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/retentionSlice';

describe('retentionSlice reducers', () => {
  describe('retention/loadRetention/pending', () => {
    it('sets loading and loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'retention/loadRetention/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('retention/loadRetention/fulfilled', () => {
    it('sets loading, applicationReports and successMetrics', () => {
      const state = Object.freeze({
        loading: true,
        applicationReports: null,
        successMetrics: {},
        validationErrors: {},
      });

      const { loading, applicationReports, successMetrics, validationErrors } = reducer(state, {
        type: 'retention/loadRetention/fulfilled',
        payload: {
          parentRetentionData: null,
          entityRetentionData: {
            applicationReports: {
              stages: {
                develop: {
                  inheritPolicy: true,
                  enablePurging: true,
                  maxAge: '3 months',
                },
              },
            },
            successMetrics: {
              inheritPolicy: true,
              enablePurging: true,
              maxAge: '1 year',
            },
          },
        },
      });

      expect(loading).toBe(false);
      expect(applicationReports).toEqual({
        stages: {
          develop: {
            inheritPolicy: true,
            enablePurging: true,
            maxAge: { trimmedValue: '3', value: '3', isPristine: true, validationErrors: null },
            maxAgeUnit: 'months',
            maxCount: { trimmedValue: '', value: '', isPristine: true, validationErrors: null },
          },
        },
      });
      expect(successMetrics).toEqual({
        inheritPolicy: true,
        enablePurging: true,
        maxAge: { trimmedValue: '1', value: '1', isPristine: true, validationErrors: null },
        maxAgeUnit: 'years',
      });
      expect(validationErrors).toEqual({
        develop: { age: null, count: null },
        successMetrics: { age: null, count: null },
      });
    });
  });

  describe('retention/loadRetention/failed', () => {
    it('sets loading and loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'retention/loadRetention/rejected',
        payload: 'some error occurred',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('some error occurred');
    });
  });
});
