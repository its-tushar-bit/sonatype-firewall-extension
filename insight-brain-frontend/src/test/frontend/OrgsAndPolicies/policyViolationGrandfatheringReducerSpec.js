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
});
