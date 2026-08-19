/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';

describe('legacyViolation reducer', () => {
  describe('legacyViolation/loadLegacyViolation/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'legacyViolation/loadLegacyViolation/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('legacyViolation/loadLegacyViolation/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        data: null,
      });

      const { loading, data } = reducer(state, {
        type: 'legacyViolation/loadLegacyViolation/fulfilled',
        payload: 'some data',
      });

      expect(loading).toBe(false);
      expect(data).toBe('some data');
    });
  });

  describe('legacyViolation/loadLegacyViolation/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'legacyViolation/loadLegacyViolation/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('legacyViolation/saveLegacyViolation/fulfilled', () => {
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
        type: 'legacyViolation/saveLegacyViolation/fulfilled',
      });
      expect(submitMaskState).toBe(true);
      expect(submitError).toBe(false);
      expect(isDirty).toBe(false);
      const { data } = reducer(state, {
        type: 'legacyViolation/loadLegacyViolation/fulfilled',
        payload: {
          enabled: true,
          allowChange: true,
        },
      });
      expect(data).toEqual({ enabled: true, allowChange: true });
    });
  });

  describe('legacyViolation/toggleOverride', () => {
    it('toggleOverride sets allowChange', () => {
      const state = Object.freeze({
        data: {
          allowOverride: false,
        },
      });
      const { data } = reducer(state, {
        type: 'legacyViolation/toggleOverride',
      });
      expect(data).toEqual({ allowOverride: true });
    });
  });

  describe('legacyViolation/setLegacViolationStatus', () => {
    it('setLegacyViolationStatus sets Legacy Violation', () => {
      const state = Object.freeze({
        data: {
          allowChange: true,
          allowOverride: true,
          enabled: false,
          inheritedFromOrganizationName: null,
        },
      });
      const { data } = reducer(state, {
        type: 'legacyViolation/setLegacyViolationStatus',
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
