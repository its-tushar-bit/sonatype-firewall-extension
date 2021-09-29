/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';

describe('componentDetailsVulnerabilitiesSlice', () => {
  const stateConstantObject = { value: 'test value' };

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          loading: false,
        },
      });

      const { vulnerabilityDetails } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/pending',
      });

      expect(vulnerabilityDetails.loading).toBe(true);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the details', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          details: null,
          loading: true,
          error: 'error',
        },
      });

      const payload = {
        identifier: 'CVE-2014-3625',
        description: 'Directory traversal vulnerability',
        categories: ['data', 'operational'],
      };

      const { vulnerabilityDetails } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled',
        payload,
      });

      expect(vulnerabilityDetails.details).toEqual({
        identifier: 'CVE-2014-3625',
        description: 'Directory traversal vulnerability',
        categories: ['data', 'operational'],
      });
      expect(vulnerabilityDetails.loading).toBe(false);
      expect(vulnerabilityDetails.error).toBe(null);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          details: null,
          loading: true,
          error: null,
        },
      });

      const { vulnerabilityDetails } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected',
        payload: 'load error',
      });

      expect(vulnerabilityDetails.loading).toBe(false);
      expect(vulnerabilityDetails.error).toBe('load error');
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          loading: false,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      });

      expect(newState.vulnerabilities.loading).toBe(true);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          data: null,
          loading: true,
          error: 'some error',
        },
      });

      const payload = {
        data: {
          securityVulnerabilities: [
            {
              refId: '1',
              severity: 8,
              status: 'status 1',
            },
            {
              refId: '2',
              severity: 9.2,
              status: 'status 2',
            },
          ],
        },
      };

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/fulfilled',
        payload,
      });
      expect(newState.vulnerabilities.data).toEqual([
        {
          refId: '1',
          severity: 8,
          status: 'status 1',
        },
        {
          refId: '2',
          severity: 9.2,
          status: 'status 2',
        },
      ]);
      expect(newState.vulnerabilities.loading).toBe(false);
      expect(newState.vulnerabilities.error).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          data: null,
          loading: true,
          error: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/rejected',
        payload: 'loadError',
      });

      expect(newState.vulnerabilities.loading).toBe(false);
      expect(newState.vulnerabilities.error).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });
});
