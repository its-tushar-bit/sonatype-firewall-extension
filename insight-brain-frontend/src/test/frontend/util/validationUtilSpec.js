/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {validateNonEmpty, validateMaxLength, hasValidationErrors} from '../../../main/frontend/util/validationUtil';

describe('validationUtil', function() {
  describe('validationNonEmpty', () => {
    it('returns null for non empty values', function() {
      expect(validateNonEmpty('test')).toBe(null);
    });

    it('returns an error message for null values', function() {
      expect(validateNonEmpty(null)).toEqual('Must be non-empty');
    });

    it('returns an error message for empty values', function() {
      expect(validateNonEmpty('')).toEqual('Must be non-empty');
    });
  });

  describe('validateMaxLength', () => {
    it('returns null for values less than or equal to maxLength', function() {
      expect(validateMaxLength(4, 'test')).toBe(null);
      expect(validateMaxLength(10, 'test')).toBe(null);
    });

    it('returns an error message for values with a length greater than maxLength', function() {
      expect(validateMaxLength(4, 'testy')).toEqual('Please enter less than 4 characters');
    });
  });

  describe('hasValidationErrors', () => {
    it('returns false for undefined or null', function() {
      expect(hasValidationErrors()).toBe(false);
      expect(hasValidationErrors(null)).toBe(false);
    });

    it('returns false for an empty array', function() {
      expect(hasValidationErrors([])).toBe(false);
    });

    it('returns true for an array with items in it', function() {
      expect(hasValidationErrors(['this is a really big problem'])).toBe(true);
    });

    it('returns true for any string even empty ones', function() {
      expect(hasValidationErrors('')).toBe(true);
      expect(hasValidationErrors('this is a really big problem')).toBe(true);
    });
  });
});
