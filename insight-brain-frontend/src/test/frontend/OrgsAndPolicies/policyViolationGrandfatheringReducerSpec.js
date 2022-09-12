/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';

describe('policyViolationGrandfathering reducer', () => {
  describe('policyViolationGrandfathering/loadPolicyViolationGrandfathering/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('policyViolationGrandfathering/loadPolicyViolationGrandfathering/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        data: null,
      });

      const { loading, data } = reducer(state, {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/fulfilled',
        payload: 'some data',
      });

      expect(loading).toBeFalse();
      expect(data).toBe('some data');
    });
  });

  describe('policyViolationGrandfathering/loadPolicyViolationGrandfathering/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('policyViolationGrandfathering/savePolicyViolationGrandfathering/fulfilled', () => {
    it('sets enabled and allowChange', () => {
      const state = Object.freeze({
        submitMaskState: null,
        submitError: false,
        isDirty: true,
        data: {
          enabled: false,
          allowChange: false,
        },
      });
      const { submitMaskState, submitError, isDirty } = reducer(state, {
        type: 'policyViolationGrandfathering/savePolicyViolationGrandfathering/fulfilled',
      });
      expect(submitMaskState).toBeTrue();
      expect(submitError).toBeFalse();
      expect(isDirty).toBeFalse();
      const { data } = reducer(state, {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/fulfilled',
        payload: {
          enabled: true,
          allowChange: true,
        },
      });
      expect(data).toEqual({ enabled: true, allowChange: true });
    });
  });

  describe('policyViolationGrandfathering/toggleOverride', () => {
    it('toggleOverride sets allowChange', () => {
      const state = Object.freeze({
        data: {
          allowOverride: false,
        },
      });
      const { data } = reducer(state, {
        type: 'policyViolationGrandfathering/toggleOverride',
      });
      expect(data).toEqual({ allowOverride: true });
    });
  });

  describe('policyViolationGrandfathering/setGrandfatheringStatus', () => {
    it('setGrandfatheringStatus sets Policy Violation Grandfathering', () => {
      const state = Object.freeze({
        data: {
          allowChange: true,
          allowOverride: true,
          enabled: false,
          inheritedFromOrganizationName: null,
        },
      });
      const { data } = reducer(state, {
        type: 'policyViolationGrandfathering/setGrandfatheringStatus',
        payload: 'enabled',
      });
      expect(data).toEqual({
        allowChange: true,
        allowOverride: true,
        enabled: true,
        inheritedFromOrganizationName: null,
      });
    });
  });
});
