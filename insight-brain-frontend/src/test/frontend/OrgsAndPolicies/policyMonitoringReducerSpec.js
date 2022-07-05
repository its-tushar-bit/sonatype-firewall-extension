/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';

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

describe('policyMonitoring reducer', () => {
  describe('policyMonitoring/loadApplicablePolicyMonitoring/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'policyMonitoring/loadApplicablePolicyMonitoring/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('policyMonitoring/loadApplicablePolicyMonitoring/fulfilled', () => {
    it('sets loading, loadError, policyMonitoringByOwner properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        policyMonitoringByOwner: undefined,
        monitoredStage: 'monitoredStage',
        originalStage: 'originalStage',
      });

      const newState = reducer(state, {
        type: 'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        payload: applicablePolicyMonitoring,
      });

      expect(newState.loading).toBeFalse();
      expect(newState.loadError).toBeNull();
      expect(newState.monitoredStage).toBeNull();
      expect(newState.originalStage).toBeNull();
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

  describe('policyMonitoring/loadApplicablePolicyMonitoring/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'policyMonitoring/loadApplicablePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('policyMonitoring/savePolicyMonitoring/pending', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'policyMonitoring/savePolicyMonitoring/pending',
      });

      expect(submitMaskState).toBeFalse();
      expect(submitError).toBeNull();
    });
  });

  describe('policyMonitoring/savePolicyMonitoring/fulfilled', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
      });

      const { submitMaskState, submitError, originalStage } = reducer(state, {
        type: 'policyMonitoring/savePolicyMonitoring/fulfilled',
        payload: { policyMonitoring: 'policy monitoring' },
      });

      expect(submitMaskState).toBeTrue();
      expect(submitError).toBeNull();
      expect(originalStage).toEqual({ policyMonitoring: 'policy monitoring' });
    });
  });

  describe('policyMonitoring/savePolicyMonitoring/rejected', () => {
    it('sets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: null,
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'policyMonitoring/savePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
    });
  });

  describe('policyMonitoring/removePolicyMonitoring/pending', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'policyMonitoring/removePolicyMonitoring/pending',
      });

      expect(submitMaskState).toBeFalse();
      expect(submitError).toBeNull();
    });
  });

  describe('policyMonitoring/removePolicyMonitoring/fulfilled', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: 'error',
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'policyMonitoring/removePolicyMonitoring/fulfilled',
      });

      expect(submitMaskState).toBeTrue();
      expect(submitError).toBeNull();
    });
  });

  describe('policyMonitoring/removePolicyMonitoring/rejected', () => {
    it('sets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: null,
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'policyMonitoring/removePolicyMonitoring/rejected',
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
    });
  });

  describe('policyMonitoring/setMonitoredStage', () => {
    it('sets monitored stage and isDirty property', () => {
      const state = Object.freeze({
        monitoredStage: undefined,
        originalStage: {
          stageName: 'Release',
          stageTypeId: 'release',
        },
        isDirty: false,
      });
      const { isDirty, monitoredStage } = reducer(state, {
        type: 'policyMonitoring/setMonitoredStage',
        payload: {
          stageName: 'Stage Release',
          stageTypeId: 'stage-release',
        },
      });

      expect(isDirty).toBeTrue();
      expect(monitoredStage).toEqual({
        stageName: 'Stage Release',
        stageTypeId: 'stage-release',
      });
    });
  });
});
