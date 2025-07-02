/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../SpecUtil';
import reducer from 'MainRoot/OrgsAndPolicies/constraintSlice';

describe('constraint reducer', () => {
  describe('constraint/updateEditConstraintId', () => {
    it('sets editConstraintMap timestamp to true', () => {
      const state = Object.freeze({
        editConstraintMap: {
          12345678: false,
        },
      });

      const { editConstraintMap } = reducer(state, {
        type: 'constraint/updateEditConstraintId',
        payload: 87654321,
      });

      expect(editConstraintMap[87654321]).toBe(true);
      expect(editConstraintMap[12345678]).toBe(false);
    });
  });

  describe('constraint/loadConstraint/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'constraint/loadConstraint/pending',
        payload: 87654321,
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('constraint/loadConstraint/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        conditionTypesMap: null,
        editConstraintMap: {},
        conditionTypes: null,
      });

      const { loading, conditionTypesMap, editConstraintMap, conditionTypes } = reducer(state, {
        type: 'constraint/loadConstraint/fulfilled',
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

      expect(loading).toBe(false);
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

  describe('constraint/loadConstraint/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'constraint/loadConstraint/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });
});
