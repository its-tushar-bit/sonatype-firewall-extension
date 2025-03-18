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

    describe('npm', () => {
      const value = {
        format: 'npm',
        packageId: initUserInput('packageId'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for npm', () => {
        expect(getCoordinatesValue(value)).toBe('npm:packageId:version');
      });
    });

    describe('cocoapods', () => {
      const value = {
        format: 'cocoapods',
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for cocoapods', () => {
        expect(getCoordinatesValue(value)).toBe('cocoapods:name:version');
      });
    });

    describe('conan', () => {
      const value = {
        format: 'conan',
        name: initUserInput('name'),
        version: initUserInput('version'),
        channel: initUserInput('channel'),
        owner: initUserInput('owner'),
      };

      it('returns combined coordinates value for conan', () => {
        expect(getCoordinatesValue(value)).toBe('conan:name:version:channel:owner');
      });
    });

    describe('conda', () => {
      const value = {
        format: 'conda',
        channel: initUserInput('channel'),
        name: initUserInput('name'),
        version: initUserInput('version'),
        build: initUserInput('build'),
        subdir: initUserInput('subdir'),
        type: initUserInput('type'),
      };

      it('returns combined coordinates value for conda', () => {
        expect(getCoordinatesValue(value)).toBe('conda:channel:name:version:build:subdir:type');
      });
    });

    describe('composer', () => {
      const value = {
        format: 'composer',
        namespace: initUserInput('namespace'),
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for composer', () => {
        expect(getCoordinatesValue(value)).toBe('composer:namespace:name:version');
      });
    });

    describe('cargo', () => {
      const value = {
        format: 'cargo',
        name: initUserInput('name'),
        version: initUserInput('version'),
        type: initUserInput('type'),
      };

      it('returns combined coordinates value for cargo', () => {
        expect(getCoordinatesValue(value)).toBe('cargo:name:version:type');
      });
    });

    describe('cran', () => {
      const value = {
        format: 'cran',
        name: initUserInput('name'),
        version: initUserInput('version'),
        type: initUserInput('type'),
      };

      it('returns combined coordinates value for cran', () => {
        expect(getCoordinatesValue(value)).toBe('cran:name:version:type');
      });
    });

    describe('gem', () => {
      const value = {
        format: 'gem',
        name: initUserInput('name'),
        version: initUserInput('version'),
        platform: initUserInput('platform'),
      };

      it('returns combined coordinates value for gem', () => {
        expect(getCoordinatesValue(value)).toBe('gem:name:version:platform');
      });
    });

    describe('golang', () => {
      const value = {
        format: 'golang',
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for golang', () => {
        expect(getCoordinatesValue(value)).toBe('golang:name:version');
      });
    });

    describe('hf-model', () => {
      const value = {
        format: 'hf-model',
        repoId: initUserInput('repoId'),
        model: initUserInput('model'),
        version: initUserInput('version'),
        extension: initUserInput('extension'),
        modelFormat: initUserInput('modelFormat'),
      };

      it('returns combined coordinates value for hf-model', () => {
        expect(getCoordinatesValue(value)).toBe('hf-model:repoId:model:version:extension:modelFormat');
      });
    });

    describe('nuget', () => {
      const value = {
        format: 'nuget',
        packageId: initUserInput('packageId'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for nuget', () => {
        expect(getCoordinatesValue(value)).toBe('nuget:packageId:version');
      });
    });

    describe('pecoff', () => {
      const value = {
        format: 'pecoff',
        name: initUserInput('name'),
        version: initUserInput('version'),
        namespace: initUserInput('namespace'),
      };

      it('returns combined coordinates value for pecoff', () => {
        expect(getCoordinatesValue(value)).toBe('pecoff:name:version:namespace');
      });
    });

    describe('pub', () => {
      const value = {
        format: 'pub',
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for pub', () => {
        expect(getCoordinatesValue(value)).toBe('pub:name:version');
      });
    });

    describe('rpm', () => {
      const value = {
        format: 'rpm',
        name: initUserInput('name'),
        version: initUserInput('version'),
        architecture: initUserInput('architecture'),
      };

      it('returns combined coordinates value for rpm', () => {
        expect(getCoordinatesValue(value)).toBe('rpm:name:version:architecture');
      });
    });

    describe('swid', () => {
      const value = {
        format: 'swid',
        namespace: initUserInput('namespace'),
        name: initUserInput('name'),
        version: initUserInput('version'),
        tag_id: initUserInput('tag_id'),
        tag_version: initUserInput('tag_version'),
        tag_creator_name: initUserInput('tag_creator_name'),
        tag_creator_regid: initUserInput('tag_creator_regid'),
        patch: initUserInput('patch'),
      };

      it('returns combined coordinates value for swid', () => {
        const actual = getCoordinatesValue(value);
        expect(actual).toBe('swid:namespace:name:version:tag_id:tag_version:tag_creator_name:tag_creator_regid:patch');
      });
    });

    describe('swift', () => {
      const value = {
        format: 'swift',
        name: initUserInput('name'),
        version: initUserInput('version'),
      };

      it('returns combined coordinates value for swift', () => {
        expect(getCoordinatesValue(value)).toBe('swift:name:version');
      });
    });
  });
});
