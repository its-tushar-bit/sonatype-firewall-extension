/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectConstraintSlice,
  selectLoadError,
  selectIsDirty,
  selectIsLoading,
  selectEditConstraintMap,
  selectConditionTypesMap,
  selectConditionTypes,
} from 'MainRoot/OrgsAndPolicies/constraintSelectors';

describe('constraintSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        constraint: {
          isDirty: false,
          loading: false,
          loadError: 'loadError',
          editConstraintMap: { 1646123499604: true },
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
        },
      },
    };
  });

  describe('selectConstraintSlice', () => {
    it('returns slice', () => {
      const mockState = {
        orgsAndPolicies: {
          constraint: null,
        },
      };

      expect(selectConstraintSlice(mockState)).toBeNull();
    });
  });

  describe('selectLoadError', () => {
    it('returns loadError', () => {
      expect(selectLoadError(mockState)).toBe('loadError');
    });
  });

  describe('selectIsDirty', () => {
    it('returns isDirty', () => {
      expect(selectIsDirty(mockState)).toBe(false);
    });
  });

  describe('selectIsLoading', () => {
    it('returns true if loading', () => {
      mockState.orgsAndPolicies.constraint.loading = true;
      expect(selectIsLoading(mockState)).toBe(true);
    });

    it('returns false if not loading', () => {
      expect(selectIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectEditConstraintMap', () => {
    it('returns editConstraintMap', () => {
      expect(selectEditConstraintMap(mockState)).toEqual({ 1646123499604: true });
    });
  });

  describe('selectConditionTypesMap', () => {
    it('returns conditionTypesMap', () => {
      expect(selectConditionTypesMap(mockState)).toEqual({
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

  describe('selectConditionTypes', () => {
    it('returns conditionTypes', () => {
      expect(selectConditionTypes(mockState)).toEqual([
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
    });
  });
});
