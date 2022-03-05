/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { conditionString } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

describe('constraintUtil', () => {
  describe('conditionString return proper condition string', () => {
    it('conditionValue is equal to AgeInDays', () => {
      const mockCondition = {
        conditionIndex: 0,
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: '730',
      };
      const mockConditionTypesMap = {
        AgeInDays: {
          autoUnquarantineSupported: false,
          enabled: true,
          id: 'AgeInDays',
          name: 'Age',
          supportedOperators: ['older than', 'younger than'],
          threatCategory: 'QUALITY',
          valueHint: 'Enter term',
          valueType: {
            allowMultiple: false,
            availableValues: null,
            dataType: 'Integer',
            id: 'AgeInDaysValueType',
          },
          valueTypeId: 'AgeInDaysValueType',
        },
      };

      const actual = conditionString(mockCondition, mockConditionTypesMap);

      expect(actual).toBe('Age older than 2 Years');
    });

    it('conditionValue is equal to Label', () => {
      const mockCondition = {
        conditionIndex: 0,
        conditionTypeId: 'Label',
        operator: 'is not',
        value: '2438cdfe428141c8b8a06fac9bc699c3',
      };
      const mockConditionTypesMap = {
        Label: {
          autoUnquarantineSupported: false,
          enabled: true,
          id: 'Label',
          name: 'Label',
          supportedOperators: ['is', 'is not'],
          threatCategory: 'OTHER',
          valueHint: null,
          valueType: {
            allowMultiple: false,
            availableValues: [
              {
                color: 'light-red',
                description: 'sdsd',
                id: '2438cdfe428141c8b8a06fac9bc699c3',
                label: 'Label Name',
                labelLowercase: 'label name',
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
              },
            ],
            dataType: 'Label',
            id: 'LabelValueType',
          },
          valueTypeId: 'LabelValueType',
        },
      };

      const actual = conditionString(mockCondition, mockConditionTypesMap);

      expect(actual).toBe('Label is not Label Name');
    });
  });
});
