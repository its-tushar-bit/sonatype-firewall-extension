/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSlice';

describe('orgsAndPoliciesConstraint reducer', () => {
  describe('orgsAndPoliciesConstraint/updateEditConstraintId', () => {
    it('sets editConstraintMap timestamp to true', () => {
      const state = Object.freeze({
        editConstraintMap: {
          12345678: false,
        },
      });

      const { editConstraintMap } = reducer(state, {
        type: 'orgsAndPoliciesConstraint/updateEditConstraintId',
        payload: 87654321,
      });

      expect(editConstraintMap[87654321]).toBeTrue();
      expect(editConstraintMap[12345678]).toBeFalse();
    });
  });

  describe('orgsAndPoliciesConstraint/loadConstraint/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesConstraint/loadConstraint/pending',
        payload: 87654321,
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('orgsAndPoliciesConstraint/loadConstraint/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        conditionTypesMap: null,
        editConstraintMap: {},
        conditionTypes: null,
      });

      const { loading, conditionTypesMap, editConstraintMap, conditionTypes } = reducer(state, {
        type: 'orgsAndPoliciesConstraint/loadConstraint/fulfilled',
        payload: {
          conditionTypes: [
            {
              enabled: true,
              valueTypeId: 'AgeInDaysValueType',
              valueHint: 'Enter term',
              autoUnquarantineSupported: false,
              threatCategory: 'QUALITY',
              name: 'Age',
              id: 'AgeInDays',
              valueTypes: {},
            },
          ],
          conditionTypesMap: {
            AgeInDays: {
              enabled: true,
              valueTypeId: 'AgeInDaysValueType',
              valueHint: 'Enter term',
              autoUnquarantineSupported: false,
              threatCategory: 'QUALITY',
              name: 'Age',
              id: 'AgeInDays',
              valueTypes: {},
            },
          },
          editConstraintMap: {},
        },
      });

      expect(loading).toBeFalse();
      expect(conditionTypes).toEqual([
        {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      ]);
      expect(editConstraintMap).toEqual({});
      expect(conditionTypesMap).toEqual({
        AgeInDays: {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      });
    });
  });

  describe('orgsAndPoliciesConstraint/loadConstraint/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesConstraint/loadConstraint/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });
});
