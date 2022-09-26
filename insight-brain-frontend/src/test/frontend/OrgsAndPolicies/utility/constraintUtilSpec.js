/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  conditionString,
  validatePatternMatchAndEmptyValue,
  getCoordinatesValue,
} from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('constraintUtil', () => {
  describe('conditionString return proper condition string', () => {
    it('conditionValue is equal to AgeInDays', () => {
      const mockCondition = {
        conditionIndex: 0,
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: {
          value: '730',
          trimmedValue: '730',
        },
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
        value: {
          value: '2438cdfe428141c8b8a06fac9bc699c3',
          trimmedValue: '2438cdfe428141c8b8a06fac9bc699c3',
        },
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

  describe('validatePatternMatchAndEmptyValue', () => {
    let validator;
    beforeEach(() => {
      validator = validatePatternMatchAndEmptyValue(/^[^:]+$/, 'not valid');
    });

    it('returns null if no value', () => {
      expect(validator('')).toBeNull();
    });

    it('returns null if value does not have deprecated symbols', () => {
      expect(validator('valid')).toBeNull();
    });

    it('returns predefined error message if value has deprecated symbols', () => {
      expect(validator('wait:')).toBe('not valid');
    });
  });

  describe('getCoordinatesValue', () => {
    describe('maven', () => {
      const value = {
        format: 'maven',
        classifier: initUserInput('classifier'),
        groupId: initUserInput('groupId'),
        artifactId: initUserInput('artifactId'),
        version: initUserInput('version'),
        extension: initUserInput('extension'),
      };

      it('returns combined coordinates value for maven', () => {
        expect(getCoordinatesValue(value)).toBe('maven:groupId:artifactId:version:extension:classifier');
      });

      it('returns combined coordinates value for maven even if classifier is empty', () => {
        value.classifier = initUserInput('');
        expect(getCoordinatesValue(value)).toBe('maven:groupId:artifactId:version:extension:');
      });
    });

    describe('a-name', () => {
      const value = {
        format: 'a-name',
        qualifier: initUserInput('qualifier'),
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for a-name', () => {
        expect(getCoordinatesValue(value)).toBe('a-name:name:qualifier:version');
      });

      it('returns combined coordinates value for a-name if qualifier is empty', () => {
        value.qualifier = initUserInput('');
        expect(getCoordinatesValue(value)).toBe('a-name:name::version');
      });
    });

    describe('pypi', () => {
      const value = {
        format: 'pypi',
        qualifier: initUserInput('qualifier'),
        name: initUserInput('name'),
        version: initUserInput('version'),
        extension: initUserInput('extension'),
      };

      it('returns combined coordinates value for pypi', () => {
        expect(getCoordinatesValue(value)).toBe('pypi:name:version:qualifier:extension');
      });

      it('returns combined coordinates value for pypi if qualifier is empty', () => {
        value.qualifier = initUserInput('');
        expect(getCoordinatesValue(value)).toBe('pypi:name:version::extension');
      });
    });
  });
});
