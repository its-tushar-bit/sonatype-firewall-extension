/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  hasValidationErrors,
  validateHostname,
  validateEmailPatternMatch,
  validateMaxLength,
  validateMinLength,
  validateNonEmpty,
  validateForm,
  validateNameCharacters,
  validateMinMax,
  validateDuplicatedValue,
  verifyFiltersAreValid,
} from 'MainRoot/util/validationUtil';

describe('validationUtil', function () {
  describe('validationNonEmpty', () => {
    it('returns null for non empty values', function () {
      expect(validateNonEmpty('test')).toBe(null);
    });

    it('returns an error message for null values', function () {
      expect(validateNonEmpty(null)).toEqual('Must be non-empty');
    });

    it('returns an error message for empty values', function () {
      expect(validateNonEmpty('')).toEqual('Must be non-empty');
    });
  });

  describe('validateMaxLength', () => {
    it('returns null for values less than or equal to maxLength', function () {
      expect(validateMaxLength(4, 'test')).toBe(null);
      expect(validateMaxLength(10, 'test')).toBe(null);
    });

    it('returns an error message for values with a length greater than maxLength', function () {
      expect(validateMaxLength(4, 'testy')).toEqual('Please enter less than 4 characters');
    });
  });

  describe('validateMinLength', () => {
    it('returns null for values longer than or equal to minLength', function () {
      expect(validateMinLength(3, '', 'abc')).toBe(null);
      expect(validateMinLength(7, '', 'ababahalamaha')).toBe(null);
    });

    it('returns default error message for values with a length lesser than minLength', function () {
      expect(validateMinLength(4, '', 'tes')).toEqual('Enter at least 4 characters to begin filtering');
    });

    it('returns predefined error message for values with a length lesser than minLength', function () {
      expect(validateMinLength(5, 'this is predefined message', 'test')).toEqual('this is predefined message');
    });
  });

  describe('hasValidationErrors', () => {
    it('returns false for undefined or null', function () {
      expect(hasValidationErrors()).toBe(false);
      expect(hasValidationErrors(null)).toBe(false);
    });

    it('returns false for an empty array', function () {
      expect(hasValidationErrors([])).toBe(false);
    });

    it('returns true for an array with items in it', function () {
      expect(hasValidationErrors(['this is a really big problem'])).toBe(true);
    });

    it('returns true for any string even empty ones', function () {
      expect(hasValidationErrors('')).toBe(true);
      expect(hasValidationErrors('this is a really big problem')).toBe(true);
    });
  });

  describe('validateHostname', () => {
    it('returns error message for empty values', function () {
      expect(validateHostname(null)).toBe('Invalid host name');
      expect(validateHostname('')).toBe('Invalid host name');
    });

    it('returns error message for non hostname strings', function () {
      expect(validateHostname('sonatype.com/host')).toBe('Invalid host name');
    });

    it('returns null for valid hostnames', function () {
      expect(validateHostname('8.5.4.5')).toBe(null);
      expect(validateHostname('sonatype.com')).toBe(null);
    });
  });

  describe(`validateEmailPatternMatch`, () => {
    const errorMessage = 'Use valid format: abc@xyz.com';
    it('allows a valid email address', () => {
      expect(validateEmailPatternMatch(errorMessage, 'john@doe.com')).toBeNull();
    });

    it('allows hyphens in email address', () => {
      expect(validateEmailPatternMatch(errorMessage, 'jane-john@doe-doe.com')).toBeNull();
    });

    it('allows subdomains in email address domain', () => {
      expect(validateEmailPatternMatch(errorMessage, 'jane@john.doe-doe.com')).toBeNull();
    });

    it('allows plus sign in email address', () => {
      expect(validateEmailPatternMatch(errorMessage, 'jane+1@doe.com')).toBeNull();
    });

    it('allows other obscure email addresses', () => {
      expect(validateEmailPatternMatch(errorMessage, 'much."more\\ unusual"@example.com')).toBeNull();
      expect(validateEmailPatternMatch(errorMessage, 'very.unusual."@".unusual.com@example.com')).toBeNull();
      expect(
        validateEmailPatternMatch(errorMessage, 'very."(),:;<>[]".VERY."very@\\\\ "very".unusual@strange.example.com')
      ).toBeNull();
      expect(validateEmailPatternMatch(errorMessage, 'quote”xyz@strange.example.com')).toBeNull();
    });

    it(`returns "${errorMessage}" error if email is invalid`, () => {
      expect(validateEmailPatternMatch(errorMessage, '@.')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, '.@')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, 's @s.c')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, 's@ s.com')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, 's@s .com')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, 's@s. com')).toBe(errorMessage);
      expect(validateEmailPatternMatch(errorMessage, 's@s.c om')).toBe(errorMessage);
    });
  });

  describe('validateNameCharacters', () => {
    it('returns error message for invalid value', () => {
      expect(validateNameCharacters('$')).toBe('Use valid characters: alphanumeric, "_", ".", "-", or spaces');
      expect(validateNameCharacters('#')).toBe('Use valid characters: alphanumeric, "_", ".", "-", or spaces');
    });

    it('returns bull for valid values', () => {
      expect(validateNameCharacters('a')).toBeNull();
      expect(validateNameCharacters('John Doe')).toBeNull();
    });
  });

  describe('validateMinMax', () => {
    const message = 'Integer between 3 and 12';

    it('returns error message if value is less than approved', () => {
      expect(validateMinMax([3, 12], message, 2)).toBe(message);
    });

    it('returns error message if value is bigger than approved', () => {
      expect(validateMinMax([3, 12], message, 13)).toBe(message);
    });

    it('returns null if value is within limits', () => {
      expect(validateMinMax([3, 12], message, 5)).toBeNull();
    });
  });

  describe('validateForm', () => {
    it('returns null if no inputs provided', () => {
      expect(validateForm({})).toBeNull();
    });

    it('returns error message if not all required fields are filled', () => {
      const inputs = {
        firstName: {
          value: '',
          validationErrors: null,
        },
        lastName: {
          value: 'Doe',
          validationErrors: [],
        },
      };

      expect(validateForm(inputs)).toBe('Unable to save: fields with invalid or missing data');
    });

    it('returns error message if some fields have validation errors', () => {
      const inputs = {
        firstName: {
          value: 'John',
          validationErrors: 'Must be non-empty',
        },
        lastName: {
          value: 'Doe',
          validationErrors: [],
        },
      };

      expect(validateForm(inputs)).toBe('Unable to save: fields with invalid or missing data');
    });
  });

  describe('validateDuplicatedValue', () => {
    const dupsArray = [
      { name: 'name 1' },
      { name: 'name 2' },
      { name: 'name 3' },
      { name: 'name 4' },
      { name: 'name 5' },
    ];

    it('returns null for no duplicates', () => {
      expect(validateDuplicatedValue(dupsArray, 'name 6')).toBeNull();
    });

    it('returns duplicate', () => {
      expect(validateDuplicatedValue(dupsArray, 'name 5')).toBe('Name is already in use');
    });

    it('returns null for current value', () => {
      expect(validateDuplicatedValue(dupsArray, 'name 5', 'name 5')).toBeNull();
    });
  });

  describe('verifyFiltersAreValid', () => {
    let componentRequestBody = {
      searchFilters: [
        {
          filterableField: 'POLICY_NAME',
          value: 'security',
        },
        {
          filterableField: 'COMPONENT_COORDINATES',
          value: 'ant',
        },
        {
          filterableField: 'QUARANTINE_TIME',
          value: '19',
        },
      ],
    };

    it('All filters are present and valid', () => {
      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(true);
    });

    it('All filters are present but not valid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 's',
          },
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'a',
          },
          {
            filterableField: 'QUARANTINE_TIME',
            value: '2',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('Policy name filter is valid and present and component filter is not present', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 'security',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(true);
    });

    it('Policy name filter is invalid and component filter is not present', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 's',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('Policy name filter is valid and component filter is invalid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 'security',
          },
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'a',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('component filter is valid and present and policy name filter is not present', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'ant',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(true);
    });

    it('component filter is invalid and policy name filter is not present', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'a',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('component filter is valid and policy name filter is invalid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'POLICY_NAME',
            value: 's',
          },
          {
            filterableField: 'COMPONENT_COORDINATES',
            value: 'ant',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('quarantine time filter is valid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'QUARANTINE_TIME',
            value: '19',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(true);
    });

    it('quarantine time filter is invalid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'QUARANTINE_TIME',
            value: '1',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });

    it('evaluation time filter is valid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'EVALUATION_TIME',
            value: '19',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(true);
    });

    it('evaluation time filter is invalid', () => {
      componentRequestBody = {
        searchFilters: [
          {
            filterableField: 'EVALUATION_TIME',
            value: '1',
          },
        ],
      };

      const filtersAreValid = verifyFiltersAreValid(componentRequestBody);
      expect(filtersAreValid).toBe(false);
    });
  });
});
