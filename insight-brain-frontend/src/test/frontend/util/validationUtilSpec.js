/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  hasValidationErrors,
  validateHostname,
  validateMaxLength,
  validateNonEmpty,
  validateForm,
  validateNameCharacters,
} from '../../../main/frontend/util/validationUtil';

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
});
