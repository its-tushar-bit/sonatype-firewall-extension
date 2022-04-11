/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSlice';

const applicablePolicyMonitoring = {
  policyMonitoringByOwner: [
    {
      ownerName: 'Root Organization',
      policyMonitoring: [
        {
          id: '8c54015dddc5465dbfb973b9979081e7',
          ownerId: 'ROOT_ORGANIZATION_ID',
          stageTypeId: 'develop',
        },
      ],
    },
  ],
};

describe('orgsAndPoliciesPolicyMonitoring reducer', () => {
  describe('orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/fulfilled', () => {
    it('sets loading, loadError, policyMonitoringByOwner properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        policyMonitoringByOwner: undefined,
      });

      const newState = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        payload: [applicablePolicyMonitoring, { policiesByOwner: [], actionStages: [] }],
      });

      expect(newState.loading).toBeFalse();
      expect(newState.loadError).toBeNull();
      expect(newState.policyMonitoringByOwner).toEqual([
        {
          ownerName: 'Root Organization',
          policyMonitoring: [
            {
              id: '8c54015dddc5465dbfb973b9979081e7',
              ownerId: 'ROOT_ORGANIZATION_ID',
              stageTypeId: 'develop',
            },
          ],
        },
      ]);
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/loadApplicablePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/pending', () => {
    it('resets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: false,
        submitError: 'error',
      });

      const { loading, submitError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/pending',
      });

      expect(loading).toBeTrue();
      expect(submitError).toBeNull();
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/fulfilled', () => {
    it('resets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: false,
        submitError: 'error',
      });

      const { loading, submitError, originalStage } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/fulfilled',
        payload: { policyMonitoring: 'policy monitoring' },
      });

      expect(loading).toBeFalse();
      expect(submitError).toBeNull();
      expect(originalStage).toEqual({ policyMonitoring: 'policy monitoring' });
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/rejected', () => {
    it('sets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: true,
        submitError: null,
      });

      const { loading, submitError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/savePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(submitError).toBe('error');
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/pending', () => {
    it('resets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: false,
        submitError: 'error',
      });

      const { loading, submitError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/pending',
      });

      expect(loading).toBeTrue();
      expect(submitError).toBeNull();
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/fulfilled', () => {
    it('resets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: false,
        submitError: 'error',
      });

      const { loading, submitError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/fulfilled',
      });

      expect(loading).toBeFalse();
      expect(submitError).toBeNull();
    });
  });

  describe('orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/rejected', () => {
    it('sets loading, submitError properties', () => {
      const state = Object.freeze({
        loading: true,
        submitError: null,
      });

      const { loading, submitError } = reducer(state, {
        type: 'orgsAndPoliciesPolicyMonitoring/removePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(submitError).toBe('error');
    });
  });
});
